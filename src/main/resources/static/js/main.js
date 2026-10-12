const state = {
	user: null,
	contacts: [],
	templates: [],
	campaigns: [],
	messages: [],
	requests: [],
	lists: []
};

const PAGE_SIZES = {
	contacts: 10,
	templates: 10,
	campaigns: 10,
	messages: 10,
	requests: 10,
	reportCampaigns: 10
};

const paginationState = {
	contacts: 1,
	templates: 1,
	campaigns: 1,
	messages: 1,
	requests: 1,
	reportCampaigns: 1
};

const filterState = {
	contacts: '',
	templates: '',
	campaigns: '',
	campaignStatus: '',
	messages: ''
};

let realtimeAbortController = null;
let realtimeReconnectTimer = null;
let loadingAll = false;

const STORAGE_KEYS = {
	accessToken: 'wcm_access_token',
	refreshToken: 'wcm_refresh_token',
	activeSection: 'wcm_active_section'
};

const AVAILABLE_PERMISSIONS = [
	'USER_VIEW', 'USER_CREATE', 'USER_UPDATE', 'USER_DELETE',
	'CONTACT_VIEW', 'CONTACT_CREATE', 'CONTACT_UPDATE', 'CONTACT_DELETE',
	'TEMPLATE_VIEW', 'TEMPLATE_CREATE', 'TEMPLATE_UPDATE', 'TEMPLATE_APPROVE', 'TEMPLATE_REJECT',
	'CAMPAIGN_VIEW', 'CAMPAIGN_CREATE', 'CAMPAIGN_UPDATE', 'CAMPAIGN_DELETE', 'CAMPAIGN_SUBMIT', 'CAMPAIGN_APPROVE', 'CAMPAIGN_REJECT',
	'MESSAGE_VIEW', 'MESSAGE_SEND', 'REPORT_VIEW', 'REPORT_EXPORT'
];

function displayLabel(value) {
	return String(value || '')
		.toLowerCase()
		.replace(/(^|[_\s-])([a-z])/g, (_, separator, letter) =>
			`${separator === '_' ? ' ' : separator}${letter.toUpperCase()}`
		);
}

function formatListName(name, fallback = 'Contact list') {
	if (!name || !String(name).trim()) return fallback;
	const trimmed = String(name).trim();
	if (/^[a-f0-9]{32,64}$/i.test(trimmed)) {
		return fallback;
	}
	return trimmed;
}

function formatContactLists(item) {
	if (item.contactLists && item.contactLists.length) {
		const names = item.contactLists
			.map(l => formatListName(l.listName || l.listId))
			.filter(Boolean);
		if (names.length) {
			return names.join(', ');
		}
	}
	if (item.listName) {
		return formatListName(item.listName);
	}
	return '—';
}

function applyProfessionalLabels() {
	const workspaceLabel = document.querySelector('.auth-aside p');
	const topbarLabel = document.querySelector('.topbar .eyebrow');
	const liveLabel = document.querySelector('.live-pill');
	const messagesBadge = document.querySelector('#section-messages .readonly-badge');

	if (workspaceLabel) workspaceLabel.textContent = 'Campaign workspace';
	if (topbarLabel) topbarLabel.textContent = 'Campaign control';
	if (liveLabel) liveLabel.lastChild.textContent = ' Connected';
	if (messagesBadge) messagesBadge.textContent = 'View only';

	['#campaignStatus', '#messageStatus'].forEach(selector => {
		document.querySelectorAll(`${selector} option`).forEach(option => {
			option.textContent = option.value ? displayLabel(option.value) : 'All statuses';
		});
	});
}

function persistAuth(user) {
	if (!user) {
		localStorage.removeItem(STORAGE_KEYS.accessToken);
		localStorage.removeItem(STORAGE_KEYS.refreshToken);
		return;
	}

	if (user.accessToken) {
		localStorage.setItem(
			STORAGE_KEYS.accessToken,
			user.accessToken
		);
	}

	if (user.refreshToken) {
		localStorage.setItem(
			STORAGE_KEYS.refreshToken,
			user.refreshToken
		);
	}
}

function clearStoredAuth() {
	localStorage.removeItem(STORAGE_KEYS.accessToken);
	localStorage.removeItem(STORAGE_KEYS.refreshToken);
	state.user = null;
}

function applyUserToView(user) {
	if (!user) {
		return;
	}

	$('#userName').textContent = user.username;
	$('#userRole').textContent = user.role || 'Workspace user';
	$('#userInitial').textContent = user.username
		.charAt(0)
		.toUpperCase();

	$('#authView').classList.add('hidden');
	$('#appView').classList.remove('hidden');
	applyAccessControl(user);
}

function hasPermission(permission) {
	return state.user?.role === 'ADMIN' ||
		(state.user?.permissions || []).includes(permission);
}

function applyAccessControl(user) {
	state.user = user;

	const sectionPermissions = {
		contacts: 'CONTACT_VIEW',
		templates: 'TEMPLATE_VIEW',
		campaigns: 'CAMPAIGN_VIEW',
		messages: 'MESSAGE_VIEW',
		requests: 'USER_VIEW',
		reports: 'REPORT_VIEW'
	};

	Object.entries(sectionPermissions).forEach(([section, permission]) => {
		const visible = hasPermission(permission);
		document.querySelectorAll(`[data-section="${section}"], #section-${section}`)
			.forEach(element => element.classList.toggle('hidden', !visible));
	});

	const controls = [
		['#section-contacts [data-open-form]', 'CONTACT_CREATE'],
		['#section-templates [data-open-form]', 'TEMPLATE_CREATE'],
		['#section-templates [data-submit-template]', 'TEMPLATE_UPDATE'],
		['#section-templates [data-delete]', 'TEMPLATE_UPDATE'],
		['#section-campaigns [data-open-form]', 'CAMPAIGN_CREATE'],
		['#section-campaigns [data-submit]', 'CAMPAIGN_SUBMIT'],
		['#section-campaigns [data-approve-campaign]', 'CAMPAIGN_APPROVE'],
		['#section-campaigns [data-reject-campaign]', 'CAMPAIGN_REJECT'],
		['#section-campaigns [data-delete]', 'CAMPAIGN_DELETE'],
		['[data-section-jump="campaigns"]', 'CAMPAIGN_CREATE']
	];

	controls.forEach(control => {
		const [selector, permission] = control;
		document.querySelectorAll(selector)
			.forEach(element => element.classList.toggle('hidden', !hasPermission(permission)));
	});
}

async function refreshAccessToken() {
	const refreshToken = localStorage.getItem(STORAGE_KEYS.refreshToken);

	if (!refreshToken) {
		return false;
	}

	try {
		const refreshed = await api('/auth/refresh', {
			method: 'POST',
			headers: {
				'Authorization': `Bearer ${refreshToken}`
			}
		});

		persistAuth(refreshed);
		return true;
	} catch (error) {
		clearStoredAuth();
		return false;
	}
}

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];

function showToast(message, error = false) {
	const toast = $('#toast');

	toast.textContent = message;

	toast.className =
		`toast show${error ? ' error' : ''}`;

	setTimeout(() => {
		toast.className = 'toast';
	}, 2800);
}

async function api(path, options = {}) {

	const isFormData =
		options.body instanceof FormData;

	const headers = isFormData
		? { ...(options.headers || {}) }
		: {
			'Content-Type': 'application/json',
			...(options.headers || {})
		};

	const storedAccessToken =
		localStorage.getItem(STORAGE_KEYS.accessToken);

	if (storedAccessToken && !headers.Authorization) {
		headers.Authorization = `Bearer ${storedAccessToken}`;
	}

	const response = await fetch(`/api${path}`, {
		headers,
		credentials: 'same-origin',
		...options
	});

	const readResponse = async responseToRead => {
		if (responseToRead.status === 204) {
			return null;
		}

		const contentType =
			responseToRead.headers.get('content-type') || '';

		return contentType.includes('application/json')
			? responseToRead.json()
			: responseToRead.text();
	};

	if (response.status === 401 && path !== '/auth/refresh') {
		const refreshed = await refreshAccessToken();

		if (refreshed) {
			const retryHeaders = (() => {
				const base = isFormData
					? { ...(options.headers || {}) }
					: { 'Content-Type': 'application/json', ...(options.headers || {}) };
				base.Authorization = `Bearer ${localStorage.getItem(STORAGE_KEYS.accessToken)}`;
				return base;
			})();

			const retryResponse = await fetch(`/api${path}`, {
				headers: retryHeaders,
				credentials: 'same-origin',
				...options
			});

			if (retryResponse.ok) {
				return readResponse(retryResponse);
			}
		}
	}

	if (!response.ok) {

		let message =
			`Request failed (${response.status})`;

		const contentType =
			response.headers.get('content-type') || '';

		if (contentType.includes('application/json')) {
			try {
				const data = await response.json();
				message = data.message || message;
			} catch (_) {
				// Keep the default message when the error body cannot be read.
			}
		} else {
			const text = await response.text();
			if (text) {
				message = text;
			}
		}

		throw new Error(message);
	}

	return readResponse(response);
}

function setSection(section) {
	const sectionElement = $(`#section-${section}`);

	if (!sectionElement) {
		return;
	}

	localStorage.setItem(STORAGE_KEYS.activeSection, section);

	$$('.nav-item')
		.forEach(item =>
			item.classList.toggle(
				'active',
				item.dataset.section === section
			)
		);

	$$('.content-section')
		.forEach(item =>
			item.classList.toggle(
				'active',
				item.id === `section-${section}`
			)
		);

	$('#pageTitle').textContent =
		section[0].toUpperCase() +
		section.slice(1);

	const requiredPermission = {
			contacts: 'CONTACT_VIEW',
			templates: 'TEMPLATE_VIEW',
			campaigns: 'CAMPAIGN_VIEW',
			messages: 'MESSAGE_VIEW',
			requests: 'USER_VIEW',
			reports: 'REPORT_VIEW'
		}[section];

	if (
		requiredPermission &&
		!hasPermission(requiredPermission)
	) {
		setSection('overview');
		return;
	}

	if (
		[
			'contacts',
			'templates',
			'campaigns',
			'messages'
		].includes(section)
	) {
		loadResource(section);
	}
}

function statusBadge(status) {
	const normalizedStatus = status || 'PENDING';

	return `
		<span class="status status-${normalizedStatus}">
			${displayLabel(normalizedStatus)}
		</span>
	`;
}

function dateValue(value) {

	return value
		? new Date(value).toLocaleDateString()
		: '—';
}

function openPermissionDialog(requestId) {
	const dialog = $('#permissionDialog');
	dialog.dataset.requestId = requestId;
	$('#approvalRole').value = 'EMPLOYEE';
	$('#permissionOptions').innerHTML = AVAILABLE_PERMISSIONS.map(permission => `
		<label class="permission-option">
			<input type="checkbox" value="${permission}">
			<span>${displayLabel(permission)}</span>
		</label>
	`).join('');
	dialog.classList.remove('hidden');
}

function closePermissionDialog() {
	$('#permissionDialog').classList.add('hidden');
}

function resetPage(section) {
	paginationState[section] = 1;
}

function clampPage(section, totalItems, pageSize) {
	const totalPages = Math.max(1, Math.ceil(totalItems / pageSize));
	const currentPage = Math.min(
		Math.max(paginationState[section] || 1, 1),
		totalPages
	);

	paginationState[section] = currentPage;

	return {
		currentPage,
		totalPages
	};
}

function paginateItems(section, items, pageSize) {
	const { currentPage, totalPages } =
		clampPage(section, items.length, pageSize);
	const start = (currentPage - 1) * pageSize;
	const visibleItems = items.slice(start, start + pageSize);

	return {
		visibleItems,
		currentPage,
		totalPages,
		totalItems: items.length,
		start,
		end: Math.min(start + pageSize, items.length)
	};
}

function renderPagination(section, totalItems, currentPage, totalPages, start, end) {
	const container = $(`#${section}Pagination`);

	if (!container) {
		return;
	}

	if (totalItems <= PAGE_SIZES[section]) {
		container.innerHTML = '';
		return;
	}

	container.innerHTML = `
		<div class="pagination-summary">
			Showing ${start + 1}-${end} of ${totalItems}
		</div>
		<div class="pagination-actions">
			<button
				class="table-action pagination-button"
				data-page-section="${section}"
				data-page-action="prev"
				${currentPage === 1 ? 'disabled' : ''}>
				Previous
			</button>
			<span class="pagination-page">
				Page ${currentPage} of ${totalPages}
			</span>
			<button
				class="table-action pagination-button"
				data-page-section="${section}"
				data-page-action="next"
				${currentPage === totalPages ? 'disabled' : ''}>
				Next
			</button>
		</div>
	`;
}

function renderPaginatedRows({
	section,
	items,
	pageSize,
	tableSelector,
	countSelector,
	emptyMessage,
	colspan,
	rowRenderer
}) {
	const { visibleItems, currentPage, totalPages, totalItems, start, end } =
		paginateItems(section, items, pageSize);

	const countLabel =
		`${totalItems} record${totalItems === 1 ? '' : 's'}`;

	if (countSelector) {
		$(countSelector).textContent =
			totalItems
				? `${countLabel} • Page ${currentPage} of ${totalPages}`
				: countLabel;
	}

	$(tableSelector).innerHTML =
		visibleItems.length
			? visibleItems.map(rowRenderer).join('')
			: `
				<tr>
					<td
						colspan="${colspan}"
						class="empty-state">
						${emptyMessage}
					</td>
				</tr>
			`;

	renderPagination(section, totalItems, currentPage, totalPages, start, end);
}

function getFilteredContacts() {
	const search = filterState.contacts.trim().toLowerCase();

	return state.contacts.filter(item =>
		!search ||
		(item.contactName || '').toLowerCase().includes(search)
	);
}

function getFilteredTemplates() {
	const search = filterState.templates.trim().toLowerCase();

	return state.templates.filter(item =>
		!search ||
		(item.templateName || '').toLowerCase().includes(search)
	);
}

function getFilteredCampaigns() {
	const search = filterState.campaigns.trim().toLowerCase();
	const status = filterState.campaignStatus;

	return state.campaigns.filter(item =>
		(!search || (item.campaignName || '').toLowerCase().includes(search)) &&
		(!status || item.status === status)
	);
}

function getFilteredMessages() {
	const status = filterState.messages;

	return state.messages.filter(item =>
		!status || item.status === status
	);
}

function listNamesForIds(listIds) {
	if (!listIds) {
		return '—';
	}

	const namesById = new Map(
		(state.lists || []).map(list => [
			String(list.listId),
			formatListName(list.listName)
		])
	);

	return String(listIds)
		.split(',')
		.map(id => id.trim())
		.filter(Boolean)
		.map(id => namesById.get(id) || 'Contact list')
		.join(', ') || '—';
}

function renderContacts(items = getFilteredContacts()) {
	renderPaginatedRows({
		section: 'contacts',
		items,
		pageSize: PAGE_SIZES.contacts,
		tableSelector: '#contactsTable',
		countSelector: '#contactCount',
		emptyMessage: 'No contacts found.',
		colspan: 5,
		rowRenderer: item => `
			<tr>
				<td>
					<strong>
						${item.contactName || 'Unnamed contact'}
					</strong>
				</td>

				<td>
					${item.contactNumbers || '—'}
				</td>

				<td>
					<strong>${formatContactLists(item)}</strong>
				</td>

				<td>
					${statusBadge(
						item.active
							? 'ACTIVE'
							: 'INACTIVE'
					)}
				</td>

				<td>
					<button
						class="table-action"
						data-delete="contacts"
						data-id="${item.contactId}">
						Delete
					</button>
				</td>
			</tr>
		`
	});
}

function renderTemplates(
	items = getFilteredTemplates()
) {
	renderPaginatedRows({
		section: 'templates',
		items,
		pageSize: PAGE_SIZES.templates,
		tableSelector: '#templatesTable',
		countSelector: '#templateCount',
		emptyMessage: 'No templates found.',
		colspan: 4,
		rowRenderer: item => `
			<tr>

				<td>
					<strong>
						${item.templateName}
					</strong>
				</td>

				<td
					class="truncate"
					title="${item.templateContent}">
					${item.templateContent}
				</td>

				<td>
					${statusBadge(item.status)}
					${
						item.failureReason
							? `<small class="muted">${item.failureReason}</small>`
							: ''
					}
				</td>

				<td>

					${
						item.status === 'DRAFT'
							? `
								<button
									class="table-action"
									data-submit-template="${item.templateId}">
									Submit
								</button>
							`
							: item.status === 'PENDING'
							? '<span class="muted">Waiting for simulator approval</span>'
							: '—'
					}

					<button
						class="table-action"
						data-delete="templates"
						data-id="${item.templateId}">
						Delete
					</button>

				</td>

			</tr>
		`
	});

	updateCampaignTemplateSelect();
}

function updateCampaignTemplateSelect() {
	const templateSelect = $('#campaignTemplateSelect');

	if (!templateSelect) {
		return;
	}

	const approvedTemplates = state.templates.filter(
		item => item.status === 'APPROVED'
	);

	templateSelect.innerHTML =
		'<option value="">Select an approved template</option>' +
		approvedTemplates
			.map(item => `
				<option value="${item.templateId}">
					${item.templateName}
				</option>
			`)
			.join('');

	templateSelect.disabled = approvedTemplates.length === 0;

	let hint = $('#campaignTemplateHint');

	if (!hint) {
		hint = document.createElement('small');
		hint.id = 'campaignTemplateHint';
		hint.className = 'form-note';
		templateSelect.insertAdjacentElement('afterend', hint);
	}

	hint.textContent = approvedTemplates.length === 0
		? 'Submit a template and wait for approval before creating a campaign.'
		: `${approvedTemplates.length} approved template${approvedTemplates.length === 1 ? '' : 's'} available.`;
}

function renderCampaigns(
	items = getFilteredCampaigns()
) {
	renderPaginatedRows({
		section: 'campaigns',
		items,
		pageSize: PAGE_SIZES.campaigns,
		tableSelector: '#campaignsTable',
		countSelector: '#campaignCount',
		emptyMessage: 'No campaigns found.',
		colspan: 6,
		rowRenderer: item => {
			let actions = '';

			if (item.status === 'DRAFT') {
				actions += `
					<button
						class="table-action"
						data-submit="${item.campaignId}">
						Submit
					</button>
				`;
			}

			if (item.status === 'PENDING') {
				actions += `
					<button
						class="table-action"
						data-approve-campaign="${item.campaignId}">
						Approve
					</button>

					<button
						class="table-action"
						data-reject-campaign="${item.campaignId}">
						Reject
					</button>
				`;
			}

			if (item.status === 'APPROVED') {
				actions += `
					<span class="muted">
						Approved
					</span>
				`;
			}

			if (item.status === 'REJECTED') {
				actions += `
					<span class="muted">
						Rejected
					</span>
				`;
			}

			if (item.status === 'DRAFT') {
				actions += `
					<button
						class="table-action"
						data-delete="campaigns"
						data-id="${item.campaignId}">
						Delete
					</button>
				`;
			}

			return `
				<tr>

					<td>
						<strong>
							${item.campaignName}
						</strong>

						<small class="muted">
							${item.senderName || 'No sender'}
						</small>
					</td>

					<td>
						${listNamesForIds(item.listId)}
					</td>

					<td>
						#${item.templateId || '—'}
					</td>

					<td>
						${statusBadge(item.status)}
					</td>

					<td>
						${dateValue(item.createdAt)}
					</td>

					<td>
						${actions}
					</td>

				</tr>
			`;
		}
	});
}

function renderMessages(
	items = getFilteredMessages()
) {
	renderPaginatedRows({
		section: 'messages',
		items,
		pageSize: PAGE_SIZES.messages,
		tableSelector: '#messagesTable',
		countSelector: '#messageCount',
		emptyMessage: 'No messages found.',
		colspan: 6,
		rowRenderer: item => `
			<tr>

				<td>
					#${item.messageId}
				</td>

				<td>
					#${item.campaignId || '—'}
				</td>

				<td>
					#${item.contactId || '—'}
				</td>

				<td
					class="truncate">
					${item.messageContent || '—'}
				</td>

				<td>
					${statusBadge(item.status)}
				</td>

				<td>
					${dateValue(item.sentAt)}
				</td>

			</tr>
		`
	});
}

function renderRequests(
	items = state.requests
) {
	renderPaginatedRows({
		section: 'requests',
		items,
		pageSize: PAGE_SIZES.requests,
		tableSelector: '#requestsTable',
		countSelector: '#requestCount',
		emptyMessage: 'No pending requests.',
		colspan: 6,
		rowRenderer: item => `
			<tr>
				<td>#${item.userId}</td>
				<td>${item.username || '—'}</td>
				<td>${item.email || '—'}</td>
				<td>${statusBadge(item.status)}</td>
				<td>${dateValue(item.createdAt)}</td>
				<td>
					<button class="table-action" data-request-approve="${item.userId}">Approve</button>
					<button class="table-action" data-request-reject="${item.userId}">Reject</button>
				</td>
			</tr>
		`
	});
}

async function loadResource(resource) {

	try {
		const endpoint = resource === 'requests'
			? '/users/pending'
			: `/${resource}`;

		state[resource] =
			await api(endpoint);

		if (resource === 'contacts') {
			renderContacts();
		}

		if (resource === 'templates') {
			renderTemplates();
		}

		if (resource === 'campaigns') {
			renderCampaigns();
		}

		if (resource === 'messages') {
			renderMessages();
		}

		if (resource === 'requests') {
			renderRequests();
		}

		const metric = {
			contacts: 'metricContacts',
			templates: 'metricTemplates',
			campaigns: 'metricCampaigns',
			messages: 'metricMessages'
		}[resource];

		if (metric) {
			$(`#${metric}`).textContent =
				state[resource].length;
		}

	} catch (error) {

		showToast(
			error.message,
			true
		);
	}
}

async function loadAll() {
	if (loadingAll) {
		return;
	}

	loadingAll = true;

	try {
	await loadLists();

	await Promise.all(
		[
			'contacts',
			'templates',
			'campaigns',
			'messages',
			'requests'
		].filter(resource => hasPermission({
			contacts: 'CONTACT_VIEW',
			templates: 'TEMPLATE_VIEW',
			campaigns: 'CAMPAIGN_VIEW',
			messages: 'MESSAGE_VIEW',
			requests: 'USER_VIEW'
		}[resource])).map(loadResource)
	);

	renderReports();
	renderDashboardInsights();
	} finally {
		loadingAll = false;
	}
}

function renderReports() {
	const messages = state.messages || [];
	const campaigns = state.campaigns || [];
	const reportSection = $('#section-reports');

	if (reportSection && !$('#reportExportRange')) {
		const heading = reportSection.querySelector('.section-heading');
		heading.querySelector('h3').textContent = 'Export reports';
		heading.querySelector('p:not(.eyebrow)').textContent =
			'Choose the campaigns to include, review the export preview, then download the Excel report.';
		heading.querySelector('.readonly-badge').textContent = 'Export only';
		reportSection.querySelector('.report-metrics')?.remove();
		reportSection.querySelector('.split-grid')?.remove();
		const actions = document.createElement('div');
		actions.className = 'report-actions';
		actions.innerHTML = `
			<label>Preview and export
				<select id="reportExportRange">
					<option value="LAST">Last campaign</option>
					<option value="LAST_10">Last 10 campaigns</option>
					<option value="ALL" selected>All previous campaigns</option>
				</select>
			</label>
			<button id="exportReportButton" class="button button-primary" type="button">Export Excel</button>
		`;
		heading.append(actions);
		$('#exportReportButton').addEventListener('click', exportReport);

		const preview = document.createElement('div');
		preview.id = 'reportExportPreview';
		preview.className = 'report-export-preview';
		reportSection.append(preview);
		$('#reportExportRange').addEventListener('change', renderExportPreview);
	}

	renderExportPreview();
	return;
}

function getExportCampaigns() {
	const range = $('#reportExportRange')?.value || 'ALL';
	const campaigns = [...(state.campaigns || [])]
		.sort((left, right) =>
			new Date(right.createdAt || 0) - new Date(left.createdAt || 0)
		);

	return range === 'LAST'
		? campaigns.slice(0, 1)
		: range === 'LAST_10'
			? campaigns.slice(0, 10)
			: campaigns;
}

function renderExportPreview() {
	const preview = $('#reportExportPreview');

	if (!preview) {
		return;
	}

	const campaigns = getExportCampaigns();
	const messages = state.messages || [];
	const rows = campaigns.map(campaign => {
		const campaignMessages = messages.filter(message =>
			message.campaignId === campaign.campaignId
		);
		const delivered = campaignMessages.filter(message =>
			message.status === 'DELIVERED'
		).length;
		const sent = campaignMessages.filter(message =>
			message.status === 'SENT'
		).length;
		const failed = campaignMessages.filter(message =>
			message.status === 'FAILED'
		).length;

		return {
			campaign,
			total: campaignMessages.length,
			delivered,
			sent,
			failed,
			rate: campaignMessages.length
				? Math.round(delivered * 100 / campaignMessages.length)
				: 0
		};
	});

	preview.innerHTML = `
		<div class="panel-heading">
			<div>
				<p class="eyebrow">Export preview</p>
				<h3>${rows.length} campaign${rows.length === 1 ? '' : 's'} will be exported</h3>
				<p class="report-preview-copy">
					The Excel file contains a Campaign Summary sheet and a Message Details sheet for this selection.
				</p>
			</div>
			<span class="panel-tag">${rows.reduce((total, row) => total + row.total, 0)} messages</span>
		</div>
		<div class="table-panel report-preview-table">
			<table>
				<thead>
					<tr>
						<th>Campaign</th>
						<th>Status</th>
						<th>Messages</th>
						<th>Delivered</th>
						<th>Sent</th>
						<th>Failed</th>
						<th>Delivery rate</th>
					</tr>
				</thead>
				<tbody>
					${rows.length
						? rows.map(row => `
							<tr>
								<td><strong>${row.campaign.campaignName}</strong></td>
								<td>${statusBadge(row.campaign.status)}</td>
								<td>${row.total}</td>
								<td>${row.delivered}</td>
								<td>${row.sent}</td>
								<td>${row.failed}</td>
								<td>${row.rate}%</td>
							</tr>
						`).join('')
						: '<tr><td colspan="7" class="empty-state">No campaigns match this export range.</td></tr>'}
				</tbody>
			</table>
		</div>
	`;
}

function renderDashboardInsights() {
	const messages = state.messages || [];
	const campaigns = state.campaigns || [];
	const contacts = state.contacts || [];
    const statuses = ['PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED'];
	const dashboard = $('#section-overview');

	if (dashboard && !$('#dashboardInsights')) {
		const insights = document.createElement('div');
		insights.id = 'dashboardInsights';
		insights.className = 'dashboard-insights';
		insights.innerHTML = `
			<div class="panel insight-panel">
				<div class="panel-heading"><div><p class="eyebrow">Campaign mix</p><h3>Lifecycle distribution</h3></div><span class="panel-tag">Live</span></div>
				<div id="dashboardCampaignChart" class="data-chart"></div>
			</div>
			<div class="panel insight-panel">
				<div class="panel-heading"><div><p class="eyebrow">Message delivery</p><h3>Status distribution</h3></div><span class="panel-tag">Live</span></div>
				<div id="dashboardMessageChart" class="data-chart"></div>
			</div>
			<div class="panel insight-panel insight-wide">
				<div class="panel-heading"><div><p class="eyebrow">Performance signal</p><h3 id="dashboardInsightTitle">Workspace insight</h3></div></div>
				<p id="dashboardInsightText" class="insight-copy"></p>
				<div id="dashboardCampaignPerformance" class="data-chart"></div>
			</div>
		`;
		dashboard.append(insights);
	}
	const statusCounts = Object.fromEntries(
		statuses.map(status => [
			status,
			messages.filter(message => message.status === status).length
		])
	);
	const delivered = statusCounts.DELIVERED + statusCounts.READ;
    const deliveryRate = messages.length
        ? Math.round((delivered / messages.length) * 100)
        : 0;
	const campaignStatuses = ['DRAFT', 'PENDING', 'APPROVED', 'REJECTED'];
	const campaignCounts = Object.fromEntries(
		campaignStatuses.map(status => [
			status,
			campaigns.filter(campaign => campaign.status === status).length
		])
	);

	const setText = (selector, value) => {
		const element = $(selector);
		if (element) {
			element.textContent = value;
		}
	};

	setText('#dashboardDraftCount', `${campaigns.filter(item => item.status === 'DRAFT').length} campaigns`);
	setText('#dashboardPendingCount', `${campaigns.filter(item => item.status === 'PENDING').length} campaigns`);
	setText('#dashboardApprovedCount', `${campaigns.filter(item => item.status === 'APPROVED').length} campaigns`);
	setText('#dashboardPendingMessages', statusCounts.PENDING);
	setText('#dashboardSentMessages', statusCounts.SENT);
	setText('#dashboardDeliveredMessages', statusCounts.DELIVERED);
	setText('#dashboardFailedMessages', statusCounts.FAILED);
	setText('#dashboardDeliveryRate', `${deliveryRate}% delivered`);
	setText('#reportDeliveryRate', `${deliveryRate}%`);
	setText('#reportListCount', new Set(contacts.map(contact => contact.listId).filter(Boolean)).size);
	setText('#reportApprovedCampaigns', campaigns.filter(item => item.status === 'APPROVED').length);
	setText('#reportFailedMessages', statusCounts.FAILED);

	const renderBars = (selector, rows, maximum) => {
		const chart = $(selector);
		if (!chart) {
			return;
		}

		chart.innerHTML = rows.length
			? rows.map(row => `
				<div class="chart-row">
					<div class="chart-label"><span>${displayLabel(row.label)}</span><strong>${row.value}</strong></div>
					<div class="chart-track"><span class="chart-bar ${row.className || ''}" style="width:${maximum ? Math.max((row.value / maximum) * 100, row.value ? 4 : 0) : 0}%"></span></div>
				</div>
			`).join('')
			: '<p class="empty-state">No data available yet.</p>';
	};

	renderBars(
		'#dashboardCampaignChart',
		campaignStatuses.map(status => ({
			label: status,
			value: campaignCounts[status],
			className: `chart-${status.toLowerCase()}`
		})),
		Math.max(...Object.values(campaignCounts), 1)
	);

	renderBars(
		'#dashboardMessageChart',
		statuses.map(status => ({
			label: status,
			value: statusCounts[status],
			className: `chart-${status.toLowerCase()}`
		})),
		Math.max(...Object.values(statusCounts), 1)
	);

	const performance = campaigns.map(campaign => {
		const campaignMessages = messages.filter(message => message.campaignId === campaign.campaignId);
		return {
			label: campaign.campaignName,
			value: campaignMessages.length
				? Math.round((campaignMessages.filter(message => message.status === 'DELIVERED').length / campaignMessages.length) * 100)
				: 0
		};
	}).sort((left, right) => right.value - left.value).slice(0, 5);

	renderBars('#dashboardCampaignPerformance', performance, 100);
    setText(
        '#dashboardInsightTitle',
        deliveryRate >= 80
            ? 'Delivery is on track'
            : 'Delivery needs attention'
    );

    setText(
        '#dashboardInsightText',
        messages.length
            ? `${delivered} of ${messages.length} messages were delivered or read (${deliveryRate}%). ${statusCounts.FAILED} failed messages and ${campaignCounts.PENDING} campaigns are currently waiting for attention.`
            : 'Approve a campaign to generate message delivery data and performance insights.'
    );

    setText('#dashboardReadMessages', statusCounts.READ);
    setText('#dashboardReadMessages', statusCounts.READ);
	const statusRows = $('#reportStatusRows');
	if (statusRows) {
		statusRows.innerHTML = statuses.map(status => `
			<div class="report-status-row">
				<span>${status}</span>
				<strong>${statusCounts[status]}</strong>
			</div>
		`).join('');
	}

	const campaignRows = $('#reportCampaignRows');
	if (campaignRows) {
		const { visibleItems, currentPage, totalPages, totalItems, start, end } =
			paginateItems('reportCampaigns', campaigns, PAGE_SIZES.reportCampaigns);

		campaignRows.innerHTML = visibleItems.length
			? visibleItems.map(campaign => {
				const campaignMessages = messages.filter(message => message.campaignId === campaign.campaignId);
				const campaignDelivered = campaignMessages.filter(message => message.status === 'DELIVERED').length;
				const campaignRate = campaignMessages.length
					? Math.round((campaignDelivered / campaignMessages.length) * 100)
					: 0;
				return `<tr><td><strong>${campaign.campaignName}</strong><small class="muted">${displayLabel(campaign.status)}</small></td><td>${campaignMessages.length}</td><td>${campaignDelivered}</td><td>${campaignRate}%</td></tr>`;
			}).join('')
			: '<tr><td colspan="4" class="empty-state">No campaign data yet.</td></tr>';

		renderPagination('reportCampaigns', totalItems, currentPage, totalPages, start, end);
	}
}

async function exportReport() {
	const range = $('#reportExportRange').value;
	const headers = {};
	const accessToken = localStorage.getItem(STORAGE_KEYS.accessToken);

	if (accessToken) {
		headers.Authorization = `Bearer ${accessToken}`;
	}

	try {
		let response = await fetch(`/api/reports/export?range=${encodeURIComponent(range)}`, {
			credentials: 'same-origin',
			headers
		});

		if (response.status === 401 && await refreshAccessToken()) {
			response = await fetch(`/api/reports/export?range=${encodeURIComponent(range)}`, {
				credentials: 'same-origin',
				headers: {
					Authorization: `Bearer ${localStorage.getItem(STORAGE_KEYS.accessToken)}`
				}
			});
		}

		if (!response.ok) {
			throw new Error(`Report export failed (${response.status})`);
		}

		const blob = await response.blob();
		const downloadUrl = URL.createObjectURL(blob);
		const link = document.createElement('a');
		link.href = downloadUrl;
		link.download = `campaign-report-${range.toLowerCase()}.xlsx`;
		link.click();
		URL.revokeObjectURL(downloadUrl);
		showToast('Excel report downloaded');
	} catch (error) {
		showToast(error.message, true);
	}
}


function startRealtimeUpdates() {
	if (realtimeAbortController) {
		return;
	}

	const controller = new AbortController();
	realtimeAbortController = controller;
	const accessToken = localStorage.getItem(STORAGE_KEYS.accessToken);

	const reconnect = () => {
		if (controller.signal.aborted) {
			return;
		}

		if (realtimeAbortController === controller) {
			realtimeAbortController = null;
		}

		if (!realtimeReconnectTimer) {
			realtimeReconnectTimer = setTimeout(() => {
				realtimeReconnectTimer = null;
				startRealtimeUpdates();
			}, 3000);
		}
	};

	fetch('/api/realtime/stream', {
		credentials: 'same-origin',
		headers: accessToken
			? { Authorization: `Bearer ${accessToken}` }
			: {},
		signal: controller.signal
	})
		.then(async response => {
			if (!response.ok || !response.body) {
				throw new Error(
					`Realtime connection failed (${response.status})`
				);
			}

			console.info('Realtime connection established');

			const reader = response.body.getReader();
			const decoder = new TextDecoder();
			let buffer = '';

			while (!controller.signal.aborted) {
				const { value, done } = await reader.read();

				if (done) {
					break;
				}

				buffer += decoder.decode(value, { stream: true });

				const events = buffer.split(/\r?\n\r?\n/);
				buffer = events.pop() || '';

				for (const event of events) {
					if (event.includes('event: data-updated')) {
						console.info('Realtime update received');
						await loadAll();
					}
				}
			}

			if (!controller.signal.aborted) {
				reconnect();
			}
		})
		.catch(error => {
			if (!controller.signal.aborted) {
				console.error('Realtime connection error:', error);
				reconnect();
			}
		})
		.finally(() => {
			if (realtimeAbortController === controller) {
				realtimeAbortController = null;
			}
		});
}

function stopRealtimeUpdates() {
	if (realtimeReconnectTimer) {
		clearTimeout(realtimeReconnectTimer);
		realtimeReconnectTimer = null;
	}

	if (realtimeAbortController) {
		realtimeAbortController.abort();
		realtimeAbortController = null;
	}
}



async function loadLists() {
    if (!hasPermission('CONTACT_VIEW') || !$('#campaignForm')) {
        return;
    }

    const listSelect = $('#campaignListSelect');

    if (!listSelect) {
        return;
    }

    try {
        const selectedValues = [...listSelect.selectedOptions]
            .map(option => option.value)
            .filter(Boolean);

        state.lists = await api('/contacts/lists');

        listSelect.innerHTML = state.lists
            .map(list => `
                <option value="${list.listId}">
                    ${formatListName(list.listName)}
                </option>
            `)
            .join('');

        [...listSelect.options].forEach(option => {
            option.selected = selectedValues.includes(option.value);
        });
    } catch (error) {
        showToast(error.message, true);
    }
}

async function login(event) {

	event.preventDefault();

	const form =
		new FormData(event.currentTarget);

	try {

		state.user =
			await api(
				'/auth/signin',
				{
					method: 'POST',
					body: JSON.stringify(
						Object.fromEntries(form)
					)
				}
			);

		persistAuth(state.user);
		applyUserToView(state.user);

		await loadAll();
		startRealtimeUpdates();

	} catch (error) {

		showToast(
			error.message,
			true
		);
	}
}

async function restoreSession() {
	const accessToken = localStorage.getItem(STORAGE_KEYS.accessToken);
	const refreshToken = localStorage.getItem(STORAGE_KEYS.refreshToken);

	if (!accessToken && !refreshToken) {
		return;
	}

	try {
		try {
			state.user = await api('/auth/me');
		} catch (error) {
			if (!await refreshAccessToken()) {
				throw error;
			}

			state.user = await api('/auth/me');
		}

		persistAuth(state.user);
		applyUserToView(state.user);
		setSection(localStorage.getItem(STORAGE_KEYS.activeSection) || 'overview');
		await loadAll();
		startRealtimeUpdates();
	} catch (error) {
		clearStoredAuth();
		$('#authView').classList.remove('hidden');
		$('#appView').classList.add('hidden');
	}
}

async function signup(event) {

	event.preventDefault();

	const form =
		event.currentTarget;

	const data =
		Object.fromEntries(
			new FormData(form)
		);

	if (
		data.password !==
		data.confirmPassword
	) {

		showToast(
			'Passwords do not match',
			true
		);

		return;
	}

	try {

		await api(
			'/auth/signup',
			{
				method: 'POST',
				body: JSON.stringify(data)
			}
		);

		form.reset();

		$('#signupPanel')
			.classList
			.add('hidden');

		$('#loginPanel')
			.classList
			.remove('hidden');

		showToast(
			'Account created. You can now sign in.'
		);

	} catch (error) {

		showToast(
			error.message,
			true
		);
	}
}


async function submitResource(event) {
    event.preventDefault();

    const form = event.currentTarget;
    const data = Object.fromEntries(new FormData(form));

    if (form.dataset.resource === 'campaigns') {
        const listSelect = $('#campaignListSelect');

        const listIds = listSelect
            ? [...listSelect.selectedOptions]
                .map(option => option.value)
                .filter(Boolean)
            : [];

        const manualContacts = [
            ...document.querySelectorAll('.campaign-manual-contact')
        ]
            .map(row => ({
                contactName: row.querySelector('[name="contactName"]').value.trim(),
                contactNumbers: row.querySelector('[name="contactNumbers"]').value.trim()
            }))
            .filter(contact => contact.contactName || contact.contactNumbers);

        if (!listIds.length && !manualContacts.length) {
            showToast('Select a contact list or add a contact', true);
            return;
        }

        if (manualContacts.some(contact =>
            !contact.contactName || !contact.contactNumbers
        )) {
            showToast('Complete both fields for every manual contact', true);
            return;
        }

        data.listIds = listIds;
        data.manualContacts = manualContacts;
        delete data.listId;

        if (data.templateId) {
            data.templateId = Number(data.templateId);
        }
    }

    try {
        const result = await api(`/${form.dataset.resource}`, {
            method: 'POST',
            body: JSON.stringify(data)
        });

        const panel = form.closest('.form-panel');

        if (panel) {
            panel.classList.add('hidden');
        }

        form.reset();

        if (form.dataset.resource === 'campaigns') {
            const contactsContainer = $('#campaignManualContacts');

            if (contactsContainer) {
                contactsContainer.innerHTML = `
                    <div class="campaign-manual-contact">
                        <input name="contactName" placeholder="Contact name" required>
                        <input name="contactNumbers" placeholder="Phone number(s)" required>
                    </div>
                `;
            }

            await loadLists();
            await loadResource('contacts');
        }

        showToast('Saved successfully');
        await loadResource(form.dataset.resource);

    } catch (error) {
        showToast(error.message, true);
    }
}

async function uploadContacts(event) {

	event.preventDefault();

	const form =
		event.currentTarget;

	const file =
		form.elements.file.files[0];

	if (!file) {
		return;
	}

	const data =
		new FormData();

	data.append(
		'file',
		file
	);

	try {

		const result =
			await api(
				'/contacts/upload',
				{
					method: 'POST',
					body: data
				}
			);

		form.reset();

		form
			.closest('.form-panel')
			.classList
			.add('hidden');

		showToast(
			result.message ||
			`Uploaded ${result.savedContacts} contacts`
		);

		await loadResource(
			'contacts'
		);
		await loadLists();

	} catch (error) {

		showToast(
			error.message,
			true
		);
	}
}

async function uploadCampaignContacts(event) {

	event.preventDefault();

	const form =
		event.currentTarget;

	const file =
		form.elements.file.files[0];

	if (!file) {
		return;
	}

	const data =
		new FormData();

	data.append(
		'file',
		file
	);

	try {

		const result =
			await api(
				'/contacts/upload',
				{
					method: 'POST',
					body: data
				}
			);

		form.reset();

		form
			.closest('.nested-form')
			.classList
			.add('hidden');

		showToast(
			result.message ||
			`Uploaded ${result.savedContacts} contacts`
		);

		await loadResource(
			'contacts'
		);
		await loadLists();

		const campaignList = $('#campaignListSelect');
		const uploadedList = campaignList && [...campaignList.options]
			.find(option => option.value === result.listHash);
		if (uploadedList) {
			uploadedList.selected = true;
		}

	} catch (error) {

		showToast(
			error.message,
			true
		);
	}
}

document.addEventListener(
	'click',
	async (event) => {

		const authView =
			event.target.closest(
				'[data-auth-view]'
			);

		if (authView) {

			$('#loginPanel')
				.classList
				.toggle(
					'hidden',
					authView.dataset.authView !== 'login'
				);

			$('#signupPanel')
				.classList
				.toggle(
					'hidden',
					authView.dataset.authView !== 'signup'
				);
		}

		const nav =
			event.target.closest(
				'[data-section], [data-section-jump]'
			);

		if (nav) {

			setSection(
				nav.dataset.section ||
				nav.dataset.sectionJump
			);

			if (
				(nav.dataset.section || nav.dataset.sectionJump) === 'campaigns'
			) {
				updateCampaignTemplateSelect();
			}
		}

		const open =
			event.target.closest(
				'[data-open-form]'
			);

		if (open) {

			$(`#${open.dataset.openForm}`)
				.classList
				.toggle('hidden');
		}

		const deleteButton =
			event.target.closest(
				'[data-delete]'
			);

		if (
			deleteButton &&
			confirm('Delete this record?')
		) {

			try {

				await api(
					`/${deleteButton.dataset.delete}/${deleteButton.dataset.id}`,
					{
						method: 'DELETE'
					}
				);

				showToast('Deleted');

				await loadResource(
					deleteButton.dataset.delete
				);

			} catch (error) {

				showToast(
					error.message,
					true
				);
			}
		}

		const submitTemplate =
			event.target.closest(
				'[data-submit-template]'
			);

		if (submitTemplate) {

			try {

				await api(
					`/templates/${submitTemplate.dataset.submitTemplate}/submit`,
					{
						method: 'PUT'
					}
				);

				showToast(
					'Template submitted for approval'
				);

				await loadResource(
					'templates'
				);

			} catch (error) {

				showToast(
					error.message,
					true
				);
			}
		}

		const submit =
			event.target.closest(
				'[data-submit]'
			);

		if (submit) {

			try {

				await api(
					`/campaigns/${submit.dataset.submit}/submit`,
					{
						method: 'PUT'
					}
				);

				showToast(
					'Campaign submitted for approval'
				);

				await loadResource(
					'campaigns'
				);

			} catch (error) {

				showToast(
					error.message,
					true
				);
			}
		}

		const approveCampaign =
			event.target.closest(
				'[data-approve-campaign]'
			);

		if (approveCampaign) {

			const campaignId =
				approveCampaign.dataset
					.approveCampaign;

			if (
				!confirm(
					'Approve this campaign and generate its messages?'
				)
			) {
				return;
			}

			try {

				await api(
					`/campaigns/${campaignId}/approve`,
					{
						method: 'PUT'
					}
				);

				showToast(
					'Campaign approved and messages generated'
				);

				await loadResource(
					'campaigns'
				);

				await loadResource(
					'messages'
				);

			} catch (error) {

				showToast(
					error.message,
					true
				);
			}
		}

		const rejectCampaign =
			event.target.closest(
				'[data-reject-campaign]'
			);

		if (rejectCampaign) {

			const campaignId =
				rejectCampaign.dataset
					.rejectCampaign;

			const comment =
				prompt(
					'Enter rejection reason:'
				);

			if (
				comment === null ||
				comment.trim() === ''
			) {
				return;
			}

			try {

				await api(
					`/campaigns/${campaignId}/reject?comment=${encodeURIComponent(comment)}`,
					{
						method: 'PUT'
					}
				);

				showToast(
					'Campaign rejected'
				);

				await loadResource(
					'campaigns'
				);

			} catch (error) {

				showToast(
					error.message,
					true
				);
			}
		}

		const approveRequest =
			event.target.closest(
				'[data-request-approve]'
			);

		if (approveRequest) {
			openPermissionDialog(
				approveRequest.dataset.requestApprove
			);
		}

		const rejectRequest =
			event.target.closest(
				'[data-request-reject]'
			);

		if (rejectRequest) {
			try {
				const requestId = rejectRequest.dataset.requestReject;
				await api(
					`/users/${requestId}/reject`,
					{
						method: 'PUT'
					}
				);
				showToast('Account request rejected');
				await loadResource('requests');
			} catch (error) {
				showToast(error.message, true);
			}
		}
	}
);


$('#addCampaignContactButton')?.addEventListener('click', () => {
    const container = $('#campaignManualContacts');

    if (!container) {
        return;
    }

    const row = document.createElement('div');
    row.className = 'campaign-manual-contact';

    row.innerHTML = `
        <input name="contactName" placeholder="Contact name" required>
        <input name="contactNumbers" placeholder="Phone number(s)" required>
        <button type="button" class="table-action remove-campaign-contact">
            Remove
        </button>
    `;

    container.appendChild(row);
});

$('#campaignManualContacts')?.addEventListener('click', event => {
    const button = event.target.closest('.remove-campaign-contact');

    if (button) {
        button.closest('.campaign-manual-contact')?.remove();
    }
});

$('#closeCampaignContactButton')?.addEventListener('click', () => {
    $('#campaignContactForm')?.classList.add('hidden');
});


$('#loginForm')
	.addEventListener(
		'submit',
		login
	);

$('#signupForm')
	.addEventListener(
		'submit',
		signup
	);

$('#bulkUploadForm')
	.addEventListener(
		'submit',
		uploadContacts
	);

$('#campaignBulkUploadForm')
	.addEventListener(
		'submit',
		uploadCampaignContacts
	);

$$(
	'.resource-form:not(#bulkUploadForm):not(#campaignBulkUploadForm)'
)
	.forEach(
		form =>
			form.addEventListener(
				'submit',
				submitResource
			)
	);

$('#logoutButton')
	.addEventListener(
		'click',
		async () => {
			try {
				await api(
					'/auth/logout',
					{
						method: 'POST'
					}
				);
			} catch (error) {
				// Local logout must still complete when the server session is stale.
			} finally {
				clearStoredAuth();
				stopRealtimeUpdates();
			}

			location.reload();
		}
	);

$('#cancelPermissionDialog')
	.addEventListener(
		'click',
		closePermissionDialog
	);


$('#confirmPermissionDialog')
	.addEventListener('click', async () => {
		const dialog = $('#permissionDialog');
		const requestId = dialog.dataset.requestId;
		const role = $('#approvalRole').value;
		const permissions = $$('#permissionOptions input[type="checkbox"]:checked')
			.map(input => input.value);

		if (!role) {
			showToast('Please select a role', true);
			return;
		}

		if (permissions.length === 0) {
			showToast('Select at least one permission', true);
			return;
		}

		try {
			await api(`/users/${requestId}/approve`, {
				method: 'PUT',
				body: JSON.stringify({
					role,
					permissions
				})
			});

			closePermissionDialog();
			showToast('Account activated successfully');
			await loadResource('requests');
			await loadResource('users');
		} catch (error) {
			showToast(error.message, true);
		}
	});


document.addEventListener('click', event => {
	const paginationButton = event.target.closest('[data-page-section][data-page-action]');

	if (!paginationButton) {
		return;
	}

	const section = paginationButton.dataset.pageSection;
	const action = paginationButton.dataset.pageAction;
	const pageSize = PAGE_SIZES[section];
	let items = [];

	switch (section) {
		case 'contacts':
			items = getFilteredContacts();
			break;
		case 'templates':
			items = getFilteredTemplates();
			break;
		case 'campaigns':
			items = getFilteredCampaigns();
			break;
		case 'messages':
			items = getFilteredMessages();
			break;
		case 'requests':
			items = state.requests;
			break;
		case 'reportCampaigns':
			items = state.campaigns;
			break;
		default:
			items = [];
	}

	const totalPages = Math.max(1, Math.ceil(items.length / pageSize));
	const nextPage = action === 'next'
		? Math.min((paginationState[section] || 1) + 1, totalPages)
		: Math.max((paginationState[section] || 1) - 1, 1);

	paginationState[section] = nextPage;

	switch (section) {
		case 'contacts':
			renderContacts();
			break;
		case 'templates':
			renderTemplates();
			break;
		case 'campaigns':
			renderCampaigns();
			break;
		case 'messages':
			renderMessages();
			break;
		case 'requests':
			renderRequests();
			break;
		case 'reportCampaigns':
			renderReports();
			break;
	}
});

applyProfessionalLabels();
restoreSession();

$('#refreshButton')
	.addEventListener(
		'click',
		loadAll
	);

$('#contactSearch')
	.addEventListener(
		'input',
		event => {
			filterState.contacts = event.target.value;
			resetPage('contacts');
			renderContacts();
		}
	);

$('#templateSearch')
	.addEventListener(
		'input',
		event => {
			filterState.templates = event.target.value;
			resetPage('templates');
			renderTemplates();
		}
	);

$('#campaignSearch')
	.addEventListener(
		'input',
		event => {
			filterState.campaigns = event.target.value;
			resetPage('campaigns');
			renderCampaigns();
		}
	);

$('#campaignStatus')
	.addEventListener(
		'change',
		event => {
			filterState.campaignStatus = event.target.value;
			resetPage('campaigns');
			renderCampaigns();
		}
	);

$('#messageStatus')
	.addEventListener(
		'change',
		event => {
			filterState.messages = event.target.value;
			resetPage('messages');
			renderMessages();
		}
	);



$$('.resource-form[data-resource]').forEach(form => {
    form.addEventListener('submit', submitResource);
});
