// Public Library Visitor & Book Borrowing System - JavaScript Core Engine

// Initial Sample Data
const INITIAL_BOOKS = [
    { id: "b1", title: "To Kill a Mockingbird", author: "Harper Lee", isbn: "978-0446310789", category: "Fiction", shelfLocation: "Shelf A-04", totalCopies: 5, availableCopies: 4 },
    { id: "b2", title: "1984", author: "George Orwell", isbn: "978-0451524935", category: "Sci-Fi", shelfLocation: "Shelf B-12", totalCopies: 4, availableCopies: 2 },
    { id: "b3", title: "Clean Code", author: "Robert C. Martin", isbn: "978-0132350884", category: "Technology", shelfLocation: "Shelf C-01", totalCopies: 3, availableCopies: 2 },
    { id: "b4", title: "The Great Gatsby", author: "F. Scott Fitzgerald", isbn: "978-0743273565", category: "Classics", shelfLocation: "Shelf A-09", totalCopies: 6, availableCopies: 6 },
    { id: "b5", title: "Sapiens: A Brief History of Humankind", author: "Yuval Noah Harari", isbn: "978-0062316097", category: "History", shelfLocation: "Shelf H-05", totalCopies: 4, availableCopies: 3 },
    { id: "b6", title: "Atomic Habits", author: "James Clear", isbn: "978-0735211292", category: "Self-Help", shelfLocation: "Shelf S-02", totalCopies: 5, availableCopies: 3 }
];

const INITIAL_VISITORS = [
    { id: "v1", name: "Aarav Patel", phone: "+1 555 010 1001", dob: "1998-04-12", email: "aarav.patel@example.com", address: "12 Maple Street" },
    { id: "v2", name: "Emma Johnson", phone: "+1 555 010 1002", dob: "2001-09-23", email: "emma.johnson@example.com", address: "45 Oak Avenue" },
    { id: "v3", name: "Liam Chen", phone: "+1 555 010 1003", dob: "1995-01-08", email: "liam.chen@example.com", address: "8 Cedar Lane" },
    { id: "v4", name: "Mia Garcia", phone: "+1 555 010 1004", dob: "2003-06-17", email: "mia.garcia@example.com", address: "71 Pine Road" },
    { id: "v5", name: "Noah Williams", phone: "+1 555 010 1005", dob: "1989-12-30", email: "noah.williams@example.com", address: "29 Birch Court" }
];

const NOW = Date.now();
const DAY_MS = 86400000;

const INITIAL_BORROWINGS = [
    { id: "br1", visitorId: "v1", visitorName: "Aarav Patel", visitorPhone: "+1 555 010 1001", bookId: "b2", bookTitle: "1984", bookAuthor: "George Orwell", borrowedAt: NOW - (3 * DAY_MS), durationDays: 14, dueDate: NOW + (11 * DAY_MS), returnedAt: 0, status: "ACTIVE" },
    { id: "br2", visitorId: "v2", visitorName: "Emma Johnson", visitorPhone: "+1 555 010 1002", bookId: "b3", bookTitle: "Clean Code", bookAuthor: "Robert C. Martin", borrowedAt: NOW - (12 * DAY_MS), durationDays: 7, dueDate: NOW - (5 * DAY_MS), returnedAt: 0, status: "ACTIVE" },
    { id: "br3", visitorId: "v3", visitorName: "Liam Chen", visitorPhone: "+1 555 010 1003", bookId: "b5", bookTitle: "Sapiens: A Brief History of Humankind", bookAuthor: "Yuval Noah Harari", borrowedAt: NOW - (5 * DAY_MS), durationDays: 21, dueDate: NOW + (16 * DAY_MS), returnedAt: 0, status: "ACTIVE" },
    { id: "br4", visitorId: "v4", visitorName: "Mia Garcia", visitorPhone: "+1 555 010 1004", bookId: "b6", bookTitle: "Atomic Habits", bookAuthor: "James Clear", borrowedAt: NOW - (10 * DAY_MS), durationDays: 7, dueDate: NOW - (3 * DAY_MS), returnedAt: NOW - 18000000, status: "RETURNED" }
];

// App State
let books = JSON.parse(localStorage.getItem("lib_books")) || INITIAL_BOOKS;
let visitors = JSON.parse(localStorage.getItem("lib_visitors")) || INITIAL_VISITORS;
let borrowings = JSON.parse(localStorage.getItem("lib_borrowings")) || INITIAL_BORROWINGS;

function saveData() {
    localStorage.setItem("lib_books", JSON.stringify(books));
    localStorage.setItem("lib_visitors", JSON.stringify(visitors));
    localStorage.setItem("lib_borrowings", JSON.stringify(borrowings));
}

// C Native Bridge Helper
function callC_CalculateFine(daysOverdue, dailyRate = 0.50) {
    if (window.AndroidBridge && typeof window.AndroidBridge.calculateOverdueFine === 'function') {
        return window.AndroidBridge.calculateOverdueFine(daysOverdue, dailyRate);
    }
    // JS Fallback if C bridge not attached
    return daysOverdue > 0 ? (daysOverdue * dailyRate) : 0.0;
}

function updateCEngineStatusUI() {
    if (window.AndroidBridge && typeof window.AndroidBridge.getLibraryEngineStatus === 'function') {
        const status = window.AndroidBridge.getLibraryEngineStatus();
        document.getElementById("engineStatusText").textContent = status;
    }
}

// Date Formatter
function formatDate(timestamp) {
    if (!timestamp || timestamp === 0) return "N/A";
    const date = new Date(timestamp);
    return date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" });
}

// Helper Overdue Calculation
function isRecordOverdue(record) {
    if (record.status === "RETURNED") return false;
    return record.dueDate > 0 && Date.now() > record.dueDate;
}

function getDaysOverdue(record) {
    if (!isRecordOverdue(record)) return 0;
    const diff = Date.now() - record.dueDate;
    return Math.max(1, Math.floor(diff / DAY_MS));
}

// Global UI Renders
function renderAll() {
    saveData();
    renderDashboard();
    renderVisitors();
    renderBooks();
    renderBorrowings();
    updateCEngineStatusUI();
}

// 1. DASHBOARD RENDER
function renderDashboard() {
    document.getElementById("currentDateBadge").textContent = new Date().toLocaleDateString("en-US", { weekday: "short", month: "short", day: "numeric" });

    document.getElementById("statVisitorsInside").textContent = visitors.length;
    document.getElementById("statVisitorsToday").textContent = `Registered in system`;

    const activeLoans = borrowings.filter(b => b.status === "ACTIVE").length;
    document.getElementById("statActiveLoans").textContent = activeLoans;

    const overdueList = borrowings.filter(b => isRecordOverdue(b));
    document.getElementById("statOverdueLoans").textContent = overdueList.length;

    const totalCopies = books.reduce((sum, b) => sum + (b.totalCopies || 1), 0);
    const availCopies = books.reduce((sum, b) => sum + (b.availableCopies || 0), 0);
    document.getElementById("statTotalBooks").textContent = totalCopies;
    document.getElementById("statAvailableCopies").textContent = `${availCopies} available`;

    const overdueContainer = document.getElementById("overdueList");
    const emptyMsg = document.getElementById("overdueEmptyMsg");

    if (overdueList.length === 0) {
        emptyMsg.style.display = "block";
        overdueContainer.innerHTML = "";
    } else {
        emptyMsg.style.display = "none";
        overdueContainer.innerHTML = overdueList.map(item => {
            const days = getDaysOverdue(item);
            const fine = callC_CalculateFine(days);
            return `
                <div class="item-card">
                    <div class="card-top">
                        <span class="card-title">${item.bookTitle}</span>
                        <span class="badge badge-red">OVERDUE (${days} days)</span>
                    </div>
                    <div class="card-subtitle">Borrower: <strong>${item.visitorName}</strong> (${item.visitorPhone})</div>
                    <div class="card-details">Due: ${formatDate(item.dueDate)} • Late Fine: <strong class="text-red">$${fine.toFixed(2)}</strong></div>
                    <div class="card-actions">
                        <button class="btn btn-outline btn-sm" onclick="returnBook('${item.id}')">↩ Return Book</button>
                    </div>
                </div>
            `;
        }).join("");
    }
}

// 2. VISITORS RENDER
function renderVisitors() {
    const query = document.getElementById("searchVisitors").value.trim().toLowerCase();
    const activeFilterChip = document.querySelector("#viewVisitors .chip.active")?.getAttribute("data-filter") || "all";

    // Count active loans per visitor
    const activeLoansMap = {};
    borrowings.filter(b => b.status === "ACTIVE").forEach(b => {
        activeLoansMap[b.visitorId] = (activeLoansMap[b.visitorId] || 0) + 1;
    });

    let filtered = visitors.filter(v => {
        const textMatch = [v.name, v.phone, v.email, v.address].some(f => (f || "").toLowerCase().includes(query));
        if (!textMatch) return false;

        const loansCount = activeLoansMap[v.id] || 0;
        if (activeFilterChip === "borrowing") return loansCount > 0;
        if (activeFilterChip === "no-borrowing") return loansCount === 0;
        return true;
    });

    const listEl = document.getElementById("visitorsList");
    const emptyEl = document.getElementById("visitorsEmptyState");

    if (filtered.length === 0) {
        emptyEl.style.display = "block";
        listEl.innerHTML = "";
    } else {
        emptyEl.style.display = "none";
        listEl.innerHTML = filtered.map(v => {
            const loansCount = activeLoansMap[v.id] || 0;
            return `
                <div class="item-card">
                    <div class="card-top">
                        <span class="card-title">${v.name}</span>
                        ${loansCount > 0 ? `<span class="badge badge-green">📖 ${loansCount} Book(s) Borrowed</span>` : `<span class="badge badge-blue">No Active Loans</span>`}
                    </div>
                    <div class="card-subtitle">Phone: ${v.phone} • Email: ${v.email}</div>
                    <div class="card-details">DOB: ${v.dob || 'N/A'} • Address: ${v.address || 'N/A'}</div>
                    <div class="card-actions">
                        <button class="btn btn-danger btn-sm" onclick="deleteVisitor('${v.id}')">Delete</button>
                        <button class="btn btn-outline btn-sm" onclick="openHistoryModal('${v.id}')">Borrow History</button>
                        <button class="btn btn-primary btn-sm" onclick="openIssueModalForVisitor('${v.id}')">📖 Issue Book</button>
                    </div>
                </div>
            `;
        }).join("");
    }
}

// 3. BOOKS RENDER
function renderBooks() {
    const query = document.getElementById("searchBooks").value.trim().toLowerCase();
    const activeCategory = document.querySelector("#viewBooks .chip.active")?.getAttribute("data-category") || "all";

    let filtered = books.filter(b => {
        const textMatch = [b.title, b.author, b.isbn, b.category, b.shelfLocation].some(f => (f || "").toLowerCase().includes(query));
        if (!textMatch) return false;

        if (activeCategory === "available") return (b.availableCopies || 0) > 0;
        if (activeCategory !== "all") return (b.category || "").toLowerCase() === activeCategory.toLowerCase();
        return true;
    });

    const listEl = document.getElementById("booksList");
    const emptyEl = document.getElementById("booksEmptyState");

    if (filtered.length === 0) {
        emptyEl.style.display = "block";
        listEl.innerHTML = "";
    } else {
        emptyEl.style.display = "none";
        listEl.innerHTML = filtered.map(b => {
            const avail = b.availableCopies || 0;
            const total = b.totalCopies || 1;
            const availText = avail > 0 ? `<span class="text-green font-bold">${avail} / ${total} Available</span>` : `<span class="text-red font-bold">0 / ${total} Available (All Issued)</span>`;
            return `
                <div class="item-card">
                    <div class="card-top">
                        <span class="card-title">${b.title}</span>
                        <span class="badge badge-blue">${b.category || 'General'}</span>
                    </div>
                    <div class="card-subtitle">by ${b.author}</div>
                    <div class="card-details">ISBN: ${b.isbn} • Location: <em>${b.shelfLocation}</em></div>
                    <div style="margin-top: 8px; font-size: 13px;">${availText}</div>
                    <div class="card-actions">
                        <button class="btn btn-danger btn-sm" onclick="deleteBook('${b.id}')">Delete</button>
                        <button class="btn btn-primary btn-sm" ${avail <= 0 ? 'disabled style="opacity: 0.5;"' : ''} onclick="openIssueModalForBook('${b.id}')">📖 Issue Book</button>
                    </div>
                </div>
            `;
        }).join("");
    }
}

// 4. BORROWINGS RENDER
function renderBorrowings() {
    const query = document.getElementById("searchBorrowings").value.trim().toLowerCase();
    const activeStatus = document.querySelector("#viewBorrowings .chip.active")?.getAttribute("data-status") || "all";

    let filtered = borrowings.filter(b => {
        const textMatch = [b.visitorName, b.visitorPhone, b.bookTitle, b.bookAuthor, b.status].some(f => (f || "").toLowerCase().includes(query));
        if (!textMatch) return false;

        if (activeStatus === "ACTIVE") return b.status === "ACTIVE";
        if (activeStatus === "OVERDUE") return isRecordOverdue(b);
        if (activeStatus === "RETURNED") return b.status === "RETURNED";
        return true;
    });

    const listEl = document.getElementById("borrowingsList");
    const emptyEl = document.getElementById("borrowingsEmptyState");

    if (filtered.length === 0) {
        emptyEl.style.display = "block";
        listEl.innerHTML = "";
    } else {
        emptyEl.style.display = "none";
        listEl.innerHTML = filtered.map(b => {
            const overdue = isRecordOverdue(b);
            let badgeHtml = '<span class="badge badge-green">ACTIVE LOAN</span>';
            let fineHtml = '';

            if (b.status === "RETURNED") {
                badgeHtml = `<span class="badge">Returned on ${formatDate(b.returnedAt)}</span>`;
            } else if (overdue) {
                const days = getDaysOverdue(b);
                const fine = callC_CalculateFine(days);
                badgeHtml = `<span class="badge badge-red">OVERDUE (${days} days)</span>`;
                fineHtml = `<div class="text-red" style="font-size: 12px; margin-top: 4px; font-weight: bold;">Late Fine Accrued: $${fine.toFixed(2)}</div>`;
            }

            return `
                <div class="item-card">
                    <div class="card-top">
                        <span class="card-title">${b.bookTitle}</span>
                        ${badgeHtml}
                    </div>
                    <div class="card-subtitle">Borrower: <strong>${b.visitorName}</strong> (${b.visitorPhone})</div>
                    <div class="card-details">Borrowed: ${formatDate(b.borrowedAt)} • Due Date: ${formatDate(b.dueDate)} (${b.durationDays} days loan)</div>
                    ${fineHtml}
                    ${b.status !== "RETURNED" ? `
                        <div class="card-actions">
                            <button class="btn btn-outline btn-sm" onclick="returnBook('${b.id}')">↩ Return Book</button>
                        </div>
                    ` : ''}
                </div>
            `;
        }).join("");
    }
}

// Actions
function deleteVisitor(id) {
    if (confirm("Delete visitor record permanently?")) {
        visitors = visitors.filter(x => x.id !== id);
        renderAll();
    }
}

function deleteBook(id) {
    if (confirm("Remove book from catalog inventory?")) {
        books = books.filter(x => x.id !== id);
        renderAll();
    }
}

function returnBook(borrowId) {
    const record = borrowings.find(x => x.id === borrowId);
    if (record) {
        record.status = "RETURNED";
        record.returnedAt = Date.now();

        // Increment book available copies
        const book = books.find(x => x.id === record.bookId);
        if (book) {
            book.availableCopies = Math.min(book.totalCopies, (book.availableCopies || 0) + 1);
        }
        renderAll();
    }
}

// Modal Controls & Handlers
function openModal(id) { document.getElementById(id).classList.add("open"); }
function closeModal(id) { document.getElementById(id).classList.remove("open"); }

document.querySelectorAll(".modal-close").forEach(btn => {
    btn.addEventListener("click", () => {
        document.querySelectorAll(".modal-overlay").forEach(m => m.classList.remove("open"));
    });
});

// Visitor Registration Form Submit
document.getElementById("formVisitor").addEventListener("submit", (e) => {
    e.preventDefault();
    const newV = {
        id: "v_" + Date.now(),
        name: document.getElementById("vName").value.trim(),
        phone: document.getElementById("vPhone").value.trim(),
        dob: document.getElementById("vDob").value,
        email: document.getElementById("vEmail").value.trim(),
        address: document.getElementById("vAddress").value.trim()
    };
    visitors.unshift(newV);
    closeModal("modalVisitor");
    e.target.reset();
    renderAll();
});

// Add Book Form Submit
document.getElementById("formBook").addEventListener("submit", (e) => {
    e.preventDefault();
    const copies = parseInt(document.getElementById("bCopies").value) || 1;
    const newB = {
        id: "b_" + Date.now(),
        title: document.getElementById("bTitle").value.trim(),
        author: document.getElementById("bAuthor").value.trim(),
        isbn: document.getElementById("bIsbn").value.trim() || "N/A",
        category: document.getElementById("bCategory").value,
        shelfLocation: document.getElementById("bShelf").value.trim() || "Shelf A-01",
        totalCopies: copies,
        availableCopies: copies
    };
    books.unshift(newB);
    closeModal("modalBook");
    e.target.reset();
    renderAll();
});

// Issue Book Flow
function populateIssueModal(preVisitorId = null, preBookId = null) {
    const vSelect = document.getElementById("issueVisitorSelect");
    const bSelect = document.getElementById("issueBookSelect");

    vSelect.innerHTML = visitors.map(v => `<option value="${v.id}" ${v.id === preVisitorId ? 'selected' : ''}>${v.name} (${v.phone})</option>`).join("");

    const availBooks = books.filter(b => (b.availableCopies || 0) > 0);
    if (availBooks.length === 0) {
        bSelect.innerHTML = `<option value="">No books available to issue</option>`;
    } else {
        bSelect.innerHTML = availBooks.map(b => `<option value="${b.id}" ${b.id === preBookId ? 'selected' : ''}>${b.title} by ${b.author} (${b.availableCopies} left)</option>`).join("");
    }

    updateDueDatePreview();
    openModal("modalIssue");
}

function updateDueDatePreview() {
    const days = parseInt(document.getElementById("issueDuration").value) || 14;
    const dueTime = Date.now() + (days * DAY_MS);
    document.getElementById("dueDatePreview").textContent = `Due Date Preview: ${formatDate(dueTime)} (${days} days loan)`;
}

document.getElementById("issueDuration").addEventListener("change", updateDueDatePreview);

document.getElementById("formIssue").addEventListener("submit", (e) => {
    e.preventDefault();
    const vId = document.getElementById("issueVisitorSelect").value;
    const bId = document.getElementById("issueBookSelect").value;
    const days = parseInt(document.getElementById("issueDuration").value) || 14;

    const v = visitors.find(x => x.id === vId);
    const b = books.find(x => x.id === bId);

    if (!v || !b) {
        alert("Please select a valid visitor and available book.");
        return;
    }

    const newBorrow = {
        id: "br_" + Date.now(),
        visitorId: v.id,
        visitorName: v.name,
        visitorPhone: v.phone,
        bookId: b.id,
        bookTitle: b.title,
        bookAuthor: b.author,
        borrowedAt: Date.now(),
        durationDays: days,
        dueDate: Date.now() + (days * DAY_MS),
        returnedAt: 0,
        status: "ACTIVE"
    };

    b.availableCopies = Math.max(0, (b.availableCopies || 1) - 1);
    borrowings.unshift(newBorrow);
    closeModal("modalIssue");
    renderAll();
});

function openIssueModalForVisitor(vId) { populateIssueModal(vId, null); }
function openIssueModalForBook(bId) { populateIssueModal(null, bId); }

function openHistoryModal(vId) {
    const v = visitors.find(x => x.id === vId);
    if (!v) return;

    document.getElementById("historyTitle").textContent = `Borrowing History: ${v.name}`;
    document.getElementById("historySubtitle").textContent = `Phone: ${v.phone} • Email: ${v.email}`;

    const vBorrowings = borrowings.filter(b => b.visitorId === vId);
    const container = document.getElementById("historyList");

    if (vBorrowings.length === 0) {
        container.innerHTML = `<div class="empty-state">No borrowing history for this visitor.</div>`;
    } else {
        container.innerHTML = vBorrowings.map(b => `
            <div class="item-card" style="margin-bottom: 8px;">
                <div class="card-title">${b.bookTitle}</div>
                <div class="card-subtitle">by ${b.bookAuthor}</div>
                <div class="card-details">Borrowed: ${formatDate(b.borrowedAt)} • Due: ${formatDate(b.dueDate)} • Status: <strong>${b.status}</strong></div>
            </div>
        `).join("");
    }

    openModal("modalHistory");
}

// Event Listeners for Tab Navigation
document.querySelectorAll(".bottom-nav .nav-item").forEach(navBtn => {
    navBtn.addEventListener("click", () => {
        const tabId = navBtn.getAttribute("data-tab");
        document.querySelectorAll(".bottom-nav .nav-item").forEach(b => b.classList.remove("active"));
        document.querySelectorAll(".tab-view").forEach(v => v.classList.remove("active"));

        navBtn.classList.add("active");
        document.getElementById(tabId).classList.add("active");
    });
});

// Filter Chips Listeners
function setupChipGroup(selector, renderFn) {
    document.querySelectorAll(selector).forEach(chip => {
        chip.addEventListener("click", () => {
            document.querySelectorAll(selector).forEach(c => c.classList.remove("active"));
            chip.classList.add("active");
            renderFn();
        });
    });
}

setupChipGroup("#viewVisitors .chip", renderVisitors);
setupChipGroup("#viewBooks .chip", renderBooks);
setupChipGroup("#viewBorrowings .chip", renderBorrowings);

// Search Inputs Listeners
document.getElementById("searchVisitors").addEventListener("input", renderVisitors);
document.getElementById("searchBooks").addEventListener("input", renderBooks);
document.getElementById("searchBorrowings").addEventListener("input", renderBorrowings);

// Action Button Listeners
document.getElementById("btnHeaderCheckIn").addEventListener("click", () => openModal("modalVisitor"));
document.getElementById("dashCheckInBtn").addEventListener("click", () => openModal("modalVisitor"));
document.getElementById("dashAddBookBtn").addEventListener("click", () => openModal("modalBook"));
document.getElementById("dashIssueBookBtn").addEventListener("click", () => populateIssueModal());
document.getElementById("btnNewVisitor").addEventListener("click", () => openModal("modalVisitor"));
document.getElementById("btnNewBook").addEventListener("click", () => openModal("modalBook"));
document.getElementById("btnNewIssue").addEventListener("click", () => populateIssueModal());

// Initialize App
renderAll();