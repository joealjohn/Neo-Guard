/**
 * NeoGuard - Frontend Application
 */

// ===========================
// State Management
// ===========================

const state = {
    currentPage: 'home',
    selectedFile: null,
    currentJobId: null,
    pollingInterval: null
};

// ===========================
// DOM Elements
// ===========================

const elements = {
    // Sidebar
    sidebar: document.getElementById('sidebar'),
    menuToggle: document.getElementById('menuToggle'),
    navItems: document.querySelectorAll('.nav-item'),
    pageTitle: document.getElementById('pageTitle'),

    // Pages
    pages: document.querySelectorAll('.page'),
    homePage: document.getElementById('homePage'),
    historyPage: document.getElementById('historyPage'),
    settingsPage: document.getElementById('settingsPage'),
    docsPage: document.getElementById('docsPage'),

    // Upload
    uploadZone: document.getElementById('uploadZone'),
    fileInput: document.getElementById('fileInput'),

    // History
    historyTable: document.getElementById('historyTable'),
    fullHistoryTable: document.getElementById('fullHistoryTable'),
    emptyHistory: document.getElementById('emptyHistory'),
    refreshHistory: document.getElementById('refreshHistory'),
    refreshHistoryFull: document.getElementById('refreshHistoryFull'),

    // File Modal
    fileModal: document.getElementById('fileModal'),
    closeModal: document.getElementById('closeModal'),
    cancelObfuscation: document.getElementById('cancelObfuscation'),
    startObfuscation: document.getElementById('startObfuscation'),
    modalFileName: document.getElementById('modalFileName'),
    modalFileSize: document.getElementById('modalFileSize'),

    // Config Inputs
    javaVersion: document.getElementById('javaVersion'),
    onlyMainPackage: document.getElementById('onlyMainPackage'),
    mainPackage: document.getElementById('mainPackage'),
    mainPackageGroup: document.getElementById('mainPackageGroup'),
    stringEncryption: document.getElementById('stringEncryption'),
    numberEncryption: document.getElementById('numberEncryption'),
    flowCondition: document.getElementById('flowCondition'),
    flowException: document.getElementById('flowException'),
    flowRange: document.getElementById('flowRange'),
    flowSwitch: document.getElementById('flowSwitch'),

    // Progress Modal
    progressModal: document.getElementById('progressModal'),
    progressFill: document.getElementById('progressFill'),
    progressStatus: document.getElementById('progressStatus'),
    logOutput: document.getElementById('logOutput'),
    progressFooter: document.getElementById('progressFooter'),
    closeProgress: document.getElementById('closeProgress'),
    downloadResult: document.getElementById('downloadResult'),

    // Toast Container
    toastContainer: document.getElementById('toastContainer')
};

// ===========================
// Navigation
// ===========================

function navigateTo(page) {
    state.currentPage = page;

    // Update active nav item
    elements.navItems.forEach(item => {
        item.classList.toggle('active', item.dataset.page === page);
    });

    // Update page title
    const titles = {
        home: 'Home',
        history: 'History',
        settings: 'Settings',
        docs: 'Documentation'
    };
    elements.pageTitle.textContent = titles[page] || 'Home';

    // Show active page
    elements.pages.forEach(p => {
        p.classList.toggle('active', p.id === `${page}Page`);
    });

    // Load page-specific data
    if (page === 'home' || page === 'history') {
        loadHistory();
    }

    // Close sidebar on mobile
    elements.sidebar.classList.remove('open');
}

// Setup navigation event listeners
elements.navItems.forEach(item => {
    item.addEventListener('click', (e) => {
        e.preventDefault();
        navigateTo(item.dataset.page);
    });
});

elements.menuToggle.addEventListener('click', () => {
    elements.sidebar.classList.toggle('open');
});

// ===========================
// File Upload
// ===========================

function setupUpload() {
    const zone = elements.uploadZone;
    const input = elements.fileInput;

    // Click to select file
    zone.addEventListener('click', () => input.click());

    // File selected
    input.addEventListener('change', (e) => {
        if (e.target.files.length > 0) {
            handleFileSelection(e.target.files[0]);
        }
    });

    // Drag and drop
    zone.addEventListener('dragover', (e) => {
        e.preventDefault();
        zone.classList.add('drag-over');
    });

    zone.addEventListener('dragleave', () => {
        zone.classList.remove('drag-over');
    });

    zone.addEventListener('drop', (e) => {
        e.preventDefault();
        zone.classList.remove('drag-over');

        if (e.dataTransfer.files.length > 0) {
            handleFileSelection(e.dataTransfer.files[0]);
        }
    });
}

function handleFileSelection(file) {
    if (!file.name.toLowerCase().endsWith('.jar')) {
        showToast('error', 'Only .jar files are supported');
        return;
    }

    state.selectedFile = file;

    // Update modal with file info
    elements.modalFileName.textContent = file.name;
    elements.modalFileSize.textContent = formatFileSize(file.size);

    // Reset and show detecting status
    elements.mainPackage.value = '';
    elements.mainPackage.placeholder = 'Detecting...';

    // Show file modal
    openModal(elements.fileModal);

    // Reset file input
    elements.fileInput.value = '';

    // Auto-detect main package
    detectMainPackage(file);
}

async function detectMainPackage(file) {
    try {
        const formData = new FormData();
        formData.append('file', file);

        const response = await fetch('/api/analyze', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (data.success && data.mainPackage) {
            elements.mainPackage.value = data.mainPackage;
            elements.mainPackage.placeholder = 'com.example.yourProject';
            showToast('success', `Detected package: ${data.mainPackage}`);
        } else {
            elements.mainPackage.placeholder = 'com.example.yourProject';
        }
    } catch (error) {
        console.error('Package detection error:', error);
        elements.mainPackage.placeholder = 'com.example.yourProject';
    }
}

// ===========================
// Modal Handling
// ===========================

function openModal(modal) {
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
}

function closeModalFn(modal) {
    modal.classList.remove('active');
    document.body.style.overflow = '';
}

// File modal close handlers
elements.closeModal.addEventListener('click', () => closeModalFn(elements.fileModal));
elements.cancelObfuscation.addEventListener('click', () => closeModalFn(elements.fileModal));

// Main package toggle
elements.onlyMainPackage.addEventListener('change', (e) => {
    elements.mainPackageGroup.style.display = e.target.checked ? 'block' : 'none';
});

// Start obfuscation
elements.startObfuscation.addEventListener('click', startObfuscation);

// Progress modal close
elements.closeProgress.addEventListener('click', () => {
    closeModalFn(elements.progressModal);
    stopPolling();
    loadHistory();
});

// Download result
elements.downloadResult.addEventListener('click', () => {
    if (state.currentJobId) {
        window.location.href = `/api/download/${state.currentJobId}`;
    }
});

// Click backdrop to close
document.querySelectorAll('.modal-backdrop').forEach(backdrop => {
    backdrop.addEventListener('click', () => {
        const modal = backdrop.parentElement;
        if (modal.id !== 'progressModal' || elements.progressFooter.style.display !== 'none') {
            closeModalFn(modal);
        }
    });
});

// ===========================
// Obfuscation
// ===========================

async function startObfuscation() {
    if (!state.selectedFile) {
        showToast('error', 'No file selected');
        return;
    }

    // Validate main package if required
    if (elements.onlyMainPackage.checked && !elements.mainPackage.value.trim()) {
        showToast('error', 'Main package is required');
        elements.mainPackage.focus();
        return;
    }

    // Build config
    const config = {
        mainPackage: elements.onlyMainPackage.checked ? elements.mainPackage.value.trim() : null,
        javaVersion: elements.javaVersion.value,
        stringEncryption: elements.stringEncryption.checked,
        numberEncryption: elements.numberEncryption.checked,
        flowCondition: elements.flowCondition.checked,
        flowException: elements.flowException.checked,
        flowRange: elements.flowRange.checked,
        flowSwitch: elements.flowSwitch.checked
    };

    // Close file modal and show progress modal
    closeModalFn(elements.fileModal);
    openModal(elements.progressModal);

    // Reset progress UI
    elements.progressFill.style.width = '10%';
    elements.progressStatus.textContent = 'Uploading file...';
    elements.progressStatus.className = 'progress-status';
    elements.logOutput.innerHTML = '<span class="log-line">Starting obfuscation...</span>';
    elements.progressFooter.style.display = 'none';

    try {
        // Create form data
        const formData = new FormData();
        formData.append('file', state.selectedFile);
        formData.append('config', JSON.stringify(config));

        // Upload and start obfuscation
        const response = await fetch('/api/obfuscate', {
            method: 'POST',
            body: formData
        });

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.error || 'Failed to start obfuscation');
        }

        state.currentJobId = data.jobId;
        elements.progressFill.style.width = '20%';
        elements.progressStatus.textContent = 'Obfuscation in progress...';
        addLogLine('Job created: ' + data.jobId);

        // Start polling for status
        startPolling();

    } catch (error) {
        console.error('Obfuscation error:', error);
        elements.progressStatus.textContent = 'Error: ' + error.message;
        elements.progressStatus.classList.add('error');
        addLogLine('Error: ' + error.message, 'error');
        elements.progressFooter.style.display = 'flex';
        elements.downloadResult.style.display = 'none';
    }
}

function startPolling() {
    if (state.pollingInterval) {
        clearInterval(state.pollingInterval);
    }

    state.pollingInterval = setInterval(checkJobStatus, 2000);
    checkJobStatus(); // Check immediately
}

function stopPolling() {
    if (state.pollingInterval) {
        clearInterval(state.pollingInterval);
        state.pollingInterval = null;
    }
}

async function checkJobStatus() {
    if (!state.currentJobId) return;

    try {
        const response = await fetch(`/api/status/${state.currentJobId}`);
        const data = await response.json();

        if (!data.success) {
            throw new Error(data.error || 'Failed to get status');
        }

        // Update progress based on status
        switch (data.status) {
            case 'PENDING':
                elements.progressFill.style.width = '30%';
                elements.progressStatus.textContent = 'Waiting in queue...';
                break;

            case 'PROCESSING':
                elements.progressFill.style.width = '60%';
                elements.progressStatus.textContent = 'Processing...';
                break;

            case 'COMPLETED':
                elements.progressFill.style.width = '100%';
                elements.progressStatus.textContent = 'Obfuscation complete!';
                elements.progressStatus.classList.add('success');
                addLogLine('Obfuscation completed successfully!', 'success');

                // Show download button
                elements.progressFooter.style.display = 'flex';
                elements.downloadResult.style.display = 'flex';

                stopPolling();
                showToast('success', 'Obfuscation completed successfully!');

                // Auto-download the obfuscated file
                setTimeout(() => {
                    window.location.href = `/api/download/${state.currentJobId}`;
                }, 500);
                break;

            case 'FAILED':
                elements.progressFill.style.width = '100%';
                elements.progressFill.style.background = 'var(--status-error)';
                elements.progressStatus.textContent = 'Obfuscation failed';
                elements.progressStatus.style.color = 'var(--status-error)';
                addLogLine('Error: ' + (data.errorMessage || 'Unknown error'), 'error');

                elements.progressFooter.style.display = 'flex';
                elements.downloadResult.style.display = 'none';

                stopPolling();
                showToast('error', 'Obfuscation failed');
                break;
        }

        // Update logs if available
        if (data.logs) {
            updateLogs(data.logs);
        }

    } catch (error) {
        console.error('Status check error:', error);
    }
}

function addLogLine(message, type = '') {
    const line = document.createElement('span');
    line.className = `log-line ${type}`;
    line.textContent = message;
    elements.logOutput.appendChild(line);
    elements.logOutput.scrollTop = elements.logOutput.scrollHeight;
}

function updateLogs(logs) {
    const lines = logs.split('\n').filter(l => l.trim());
    elements.logOutput.innerHTML = '';
    lines.forEach(line => {
        const span = document.createElement('span');
        span.className = 'log-line';
        span.textContent = line;
        elements.logOutput.appendChild(span);
    });
    elements.logOutput.scrollTop = elements.logOutput.scrollHeight;
}

// ===========================
// History
// ===========================

async function loadHistory() {
    try {
        const response = await fetch('/api/history?limit=50');
        const data = await response.json();

        if (!data.success) {
            throw new Error(data.error || 'Failed to load history');
        }

        renderHistory(data.jobs, elements.historyTable, 5);
        renderHistory(data.jobs, elements.fullHistoryTable);

        // Show/hide empty state
        elements.emptyHistory.style.display = data.jobs.length === 0 ? 'block' : 'none';

    } catch (error) {
        console.error('Load history error:', error);
        showToast('error', 'Failed to load history');
    }
}

function renderHistory(jobs, container, limit = null) {
    const displayJobs = limit ? jobs.slice(0, limit) : jobs;

    container.innerHTML = displayJobs.map(job => `
        <tr>
            <td>
                <div class="file-icon">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                    </svg>
                </div>
            </td>
            <td>${escapeHtml(job.originalFilename || '-')}</td>
            <td>
                <span class="status-badge ${job.status.toLowerCase()}">
                    ${job.status}
                </span>
            </td>
            <td>${job.outputSize || '-'}</td>
            <td>${formatDate(job.createdAt)}</td>
            <td>
                ${job.status === 'COMPLETED' ? `
                    <a href="/api/download/${job.id}" class="btn btn-download">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                            <polyline points="7 10 12 15 17 10"/>
                            <line x1="12" y1="15" x2="12" y2="3"/>
                        </svg>
                        Download
                    </a>
                ` : '-'}
            </td>
        </tr>
    `).join('');
}

elements.refreshHistory.addEventListener('click', loadHistory);
elements.refreshHistoryFull.addEventListener('click', loadHistory);

// ===========================
// Toast Notifications
// ===========================

function showToast(type, message) {
    const icons = {
        success: `<svg class="toast-icon success" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
        </svg>`,
        error: `<svg class="toast-icon error" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="15" y1="9" x2="9" y2="15"/>
            <line x1="9" y1="9" x2="15" y2="15"/>
        </svg>`,
        info: `<svg class="toast-icon info" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="16" x2="12" y2="12"/>
            <line x1="12" y1="8" x2="12.01" y2="8"/>
        </svg>`
    };

    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `
        ${icons[type] || icons.info}
        <span class="toast-message">${escapeHtml(message)}</span>
    `;

    elements.toastContainer.appendChild(toast);

    // Remove after animation
    setTimeout(() => {
        toast.remove();
    }, 5000);
}

// ===========================
// Utility Functions
// ===========================

function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateString) {
    if (!dateString) return '-';
    try {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return dateString;
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ===========================
// Server Health Check
// ===========================

async function checkServerHealth() {
    const statusIndicator = document.getElementById('serverStatus');
    const dot = statusIndicator.querySelector('.status-dot');
    const text = statusIndicator.querySelector('span:last-child');

    try {
        const response = await fetch('/api/health');
        const data = await response.json();

        if (data.status === 'ok') {
            dot.style.background = 'var(--status-success)';
            text.textContent = 'Connected';
        } else {
            throw new Error('Server not healthy');
        }
    } catch {
        dot.style.background = 'var(--status-error)';
        text.textContent = 'Disconnected';
    }
}

// ===========================
// Initialization
// ===========================

document.addEventListener('DOMContentLoaded', () => {
    setupUpload();
    loadHistory();
    checkServerHealth();

    // Periodic health check
    setInterval(checkServerHealth, 30000);
});
