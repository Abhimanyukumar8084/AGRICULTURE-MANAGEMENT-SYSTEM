(() => {
    "use strict";

    const apiHost = window.location.hostname || "localhost";
    const apiBase = window.DIGITAL_AGRICULTURE_API || `${window.location.protocol}//${apiHost}:8080/api`;
    const file = window.location.pathname.split("/").pop().toLowerCase();
    let csrfToken;
    let currentUser;

    document.addEventListener("click", event => {
        const toggle = event.target.closest("[data-password-toggle]");
        if (!toggle) return;

        const input = toggle.parentElement.querySelector('input[type="password"], input[type="text"]');
        if (!input) return;

        const visible = input.type === "password";
        input.type = visible ? "text" : "password";
        toggle.dataset.visible = String(visible);
        toggle.setAttribute("aria-pressed", String(visible));
        toggle.setAttribute("aria-label", `${visible ? "Hide" : "Show"} password`);
    });

    const roleLanding = { farmer: "dashboard.html", buyer: "market.html", expert: "expert-dashboard.html", admin: "admin-dashboard.html" };
    const pageRoles = {
        "dashboard.html": ["farmer", "buyer"],
        "crops.html": ["farmer"],
        "market.html": ["farmer", "buyer"],
        "weather.html": ["farmer", "buyer"],
        "activities.html": ["farmer"],
        "settings.html": ["farmer", "buyer", "expert", "admin"],
        "expert-dashboard.html": ["expert"],
        "expert-consult.html": ["expert"],
        "farmer-queries.html": ["expert"],
        "previous-consultations.html": ["farmer", "expert"],
        "expert-profile.html": ["expert"],
        "admin-dashboard.html": ["admin"],
        "admin-users.html": ["admin"],
        "admin-farmers.html": ["admin"],
        "admin-crops.html": ["admin"],
        "add-crop.html": ["admin"],
        "add-farmer.html": ["admin"],
        "admin-market.html": ["admin"],
        "admin-feedback.html": ["admin"],
        "admin-reports.html": ["admin"],
        "admin-profile.html": ["admin"],
        "admin-settings.html": ["admin"]
    };

    function escapeHtml(value) {
        return String(value ?? "").replace(/[&<>"']/g, character => ({
            "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
        })[character]);
    }

    function toast(message, kind = "success") {
        let element = document.querySelector(".app-toast");
        if (!element) {
            element = document.createElement("div");
            element.className = "app-toast";
            element.setAttribute("role", "status");
            document.body.append(element);
        }
        element.dataset.kind = kind;
        element.textContent = message;
        window.clearTimeout(element.timeoutId);
        element.timeoutId = window.setTimeout(() => element.remove(), 4200);
    }

    async function request(path, options = {}) {
        const method = (options.method || "GET").toUpperCase();
        const headers = new Headers(options.headers || {});
        if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
        if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
            if (!csrfToken) {
                const csrf = await fetch(`${apiBase}/csrf`, { credentials: "include" });
                if (!csrf.ok) throw new Error("The server is unavailable. Start the Java backend and try again.");
                csrfToken = (await csrf.json()).token;
            }
            headers.set("X-XSRF-TOKEN", csrfToken);
        }
        const response = await fetch(`${apiBase}${path}`, { ...options, method, headers, credentials: "include" });
        if (response.status === 204) return null;
        const text = await response.text();
        let result = {};
        if (text) {
            try { result = JSON.parse(text); } catch { result = { error: text }; }
        }
        if (!response.ok) {
            const fieldError = result.fields && Object.values(result.fields)[0];
            const error = new Error(fieldError || result.message || result.error || `Request failed (${response.status}).`);
            error.status = response.status;
            throw error;
        }
        return result;
    }

    function recordCard(record, { closable = false, removable = false } = {}) {
        const details = [
            record.quantity != null ? `${record.quantity} ${record.unit || ""}`.trim() : "",
            record.price != null ? `₹${Number(record.price).toLocaleString("en-IN")}` : "",
            record.location || "",
            record.dueDate || ""
        ].filter(Boolean);
        return `<article class="app-record-card" data-record-id="${escapeHtml(record.id)}">
            <h3>${escapeHtml(record.title)}</h3>
            ${record.category ? `<span class="app-status">${escapeHtml(record.category)}</span>` : ""}
            <p>${escapeHtml(record.description || "No additional details added.")}</p>
            <div class="app-record-meta">${details.map(item => `<span>${escapeHtml(item)}</span>`).join("")}</div>
            <span class="app-status">${escapeHtml(record.status || "ACTIVE")}</span>
            ${closable ? `<button class="app-button app-button-secondary" data-close-listing="${escapeHtml(record.id)}">Mark as sold</button>` : ""}
            ${record.kind === "ACTIVITY" && record.status !== "COMPLETED" && record.status !== "CANCELLED"
                ? `<button class="app-button app-button-secondary" data-complete-activity="${escapeHtml(record.id)}">Mark complete</button>` : ""}
            ${removable ? `<button class="app-button app-button-secondary" data-delete-record="${escapeHtml(record.id)}">Remove</button>` : ""}
        </article>`;
    }

    async function fetchAllAdminUsers() {
        const users = [];
        let totalPages = 1;
        for (let page = 0; page < totalPages; page += 1) {
            const result = await request(`/admin/users?size=100&page=${page}&sort=createdAt,desc`);
            if (!Array.isArray(result.content) || !Number.isInteger(result.totalPages)) {
                throw new Error("The server returned an invalid account-list response.");
            }
            users.push(...result.content);
            totalPages = result.totalPages;
        }
        return users;
    }

    function mountPanel(main, id, title, intro, actionText, action) {
        let panel = document.getElementById(id);
        if (!panel) {
            panel = document.createElement("section");
            panel.id = id;
            panel.className = "app-feature-panel";
            main.append(panel);
        }
        panel.innerHTML = `<div class="app-toolbar">
            <div><h2>${escapeHtml(title)}</h2><p>${escapeHtml(intro)}</p></div>
            ${actionText ? `<button class="app-button" data-action="${escapeHtml(action)}">${escapeHtml(actionText)}</button>` : ""}
        </div><div class="app-record-grid" data-records></div>`;
        return panel;
    }

    function openRecordDialog(kind, title) {
        document.querySelector(".app-overlay")?.remove();
        const overlay = document.createElement("div");
        overlay.className = "app-overlay";
        overlay.innerHTML = `<section class="app-dialog" role="dialog" aria-modal="true" aria-labelledby="record-title">
            <h2 id="record-title">${escapeHtml(title)}</h2>
            <p>Keep your farm information organized and up to date.</p>
            <form data-record-form>
                <input type="hidden" name="kind" value="${escapeHtml(kind)}">
                <label>Title<input name="title" maxlength="120" required placeholder="${kind === "CROP" ? "e.g. Wheat" : kind === "LISTING" ? "e.g. Fresh onions" : "e.g. Irrigate north field"}"></label>
                ${kind === "CROP" ? `<label>Crop variety<input name="category" maxlength="200" placeholder="Optional variety"></label>
                    <label>Farm area<input type="number" name="quantity" min="0.01" step="0.01" required placeholder="Area"></label>
                    <label>Area unit<select name="unit"><option>Acres</option><option>Hectares</option></select></label>
                    <label>Planting date<input type="date" name="dueDate"></label>` : ""}
                ${kind === "ACTIVITY" ? `<label>Activity type<select name="category"><option>Irrigation</option><option>Fertilizing</option><option>Planting</option><option>Harvesting</option><option>Field inspection</option><option>Other</option></select></label>
                    <label>Due date<input type="date" name="dueDate"></label>` : ""}
                ${kind === "FEEDBACK" ? `<label>Feedback topic<select name="category"><option>Product suggestion</option><option>Bug report</option><option>Market data</option><option>Other</option></select></label>` : ""}
                ${kind === "LISTING" ? `<label>Quantity<input type="number" name="quantity" min="0.01" step="0.01" required></label>
                    <label>Unit<input name="unit" maxlength="32" required placeholder="Quintal, kg, crates"></label>
                    <label>Price per unit (₹)<input type="number" name="price" min="0.01" step="0.01" required></label>
                    <label>Market / location<input name="location" maxlength="120" required></label>` : ""}
                <label>${kind === "FEEDBACK" ? "Your feedback" : "Description"}<textarea name="description" maxlength="3000" ${kind === "FEEDBACK" ? "required minlength=\"10\"" : ""} placeholder="${kind === "FEEDBACK" ? "Tell us how we can improve" : "Add useful details"}"></textarea></label>
                <div class="app-dialog-actions">
                    <button type="button" class="app-button app-button-secondary" data-close-dialog>Cancel</button>
                    <button type="submit" class="app-button">Save</button>
                </div>
            </form>
        </section>`;
        overlay.addEventListener("click", event => {
            if (event.target === overlay || event.target.closest("[data-close-dialog]")) overlay.remove();
        });
        overlay.querySelector("form").addEventListener("submit", async event => {
            event.preventDefault();
            const form = event.currentTarget;
            const submit = form.querySelector('[type="submit"]');
            submit.disabled = true;
            const data = Object.fromEntries(new FormData(form));
            data.quantity = data.quantity ? Number(data.quantity) : null;
            data.price = data.price ? Number(data.price) : null;
            data.dueDate = data.dueDate || null;
            data.status = kind === "ACTIVITY" ? "PLANNED" : "ACTIVE";
            try {
                await request("/records", { method: "POST", body: JSON.stringify(data) });
                overlay.remove();
                toast("Saved successfully.");
                await renderFarmRecords();
            } catch (error) {
                toast(error.message, "error");
                submit.disabled = false;
            }
        });
        document.body.append(overlay);
        overlay.querySelector("input:not([type=hidden])")?.focus();
    }

    async function renderFarmRecords() {
        const main = document.querySelector("main");
        if (!main || !currentUser) return;
        if (file === "dashboard.html") {
            document.querySelectorAll(".dashboard-stats .stat-card strong").forEach(value => value.textContent = "—");
            document.querySelector(".dashboard-grid .weather-card")?.remove();
            document.querySelector(".dashboard-card.advice-card")?.remove();
            const initialCropList = document.querySelector(".crop-list");
            if (initialCropList) initialCropList.innerHTML = `<div class="app-empty">Loading your crop records…</div>`;
            const initialMarketTable = document.querySelector(".dashboard-card.market-card .market-table");
            if (initialMarketTable) initialMarketTable.innerHTML = `<div class="app-empty">Loading saved market prices…</div>`;
            const crops = await request("/records?kind=CROP");
            const activities = await request("/records?kind=ACTIVITY");
            const stats = document.querySelectorAll(".dashboard-stats .stat-card");
            stats.forEach(card => {
                const label = card.querySelector("span")?.textContent.trim();
                const value = card.querySelector("strong");
                const sub = card.querySelector("small");
                if (!value) return;
                if (label === "Total Crops") {
                    value.textContent = String(crops.length);
                    if (sub) sub.textContent = "Saved crop records";
                } else if (label === "Farm Health") {
                    const completed = activities.filter(item => item.status === "COMPLETED").length;
                    const percent = activities.length ? Math.round(completed * 100 / activities.length) : 0;
                    value.textContent = `${percent}%`;
                    if (sub) sub.textContent = `${completed} of ${activities.length} activities completed`;
                } else if (label === "Market Price") {
                    value.textContent = "—";
                    if (sub) sub.textContent = "No market report loaded";
                }
            });
            const activityStat = [...stats].find(card => card.querySelector("span")?.textContent.trim() === "Farm Health");
            if (activityStat) activityStat.querySelector("span").textContent = "Activity Completion";
            document.querySelector(".dashboard-grid .weather-card")?.remove();
            const list = document.querySelector(".crop-list");
            if (list) list.innerHTML = crops.length ? crops.slice(0, 4).map(crop => `
                <div class="crop-item"><div class="crop-image">🌱</div>
                <div class="crop-info"><strong>${escapeHtml(crop.title)}</strong><span>${escapeHtml(crop.quantity ?? "—")} ${escapeHtml(crop.unit || "")}</span></div>
                <div class="crop-status healthy">${escapeHtml(crop.status)}</div></div>`).join("")
                : `<div class="app-empty">Add your first crop to see your farm overview here.</div>`;
            const marketRows = await request(`/market/prices${marketPriceQuery() ? `?${marketPriceQuery()}` : ""}`);
            const marketTable = document.querySelector(".dashboard-card.market-card .market-table");
            if (marketTable) {
                const summaryCard = [...stats].find(card => card.querySelector("span")?.textContent.trim() === "Market Price");
                if (summaryCard) {
                    summaryCard.querySelector("strong").textContent = marketRows.length ? formatRupees(marketRows[0].modalPrice) : "—";
                    const sub = summaryCard.querySelector("small");
                    if (sub) sub.textContent = marketRows.length ? `${marketRows[0].commodity} · ${marketRows[0].arrivalDate}` : "No AGMARKNET data in database";
                }
                marketTable.innerHTML = marketRows.length ? `<div class="market-row market-heading"><span>Crop</span><span>Modal price</span><span>Arrival</span></div>
                    ${marketRows.slice(0, 5).map(item => `<div class="market-row"><span>${escapeHtml(item.commodity)}</span>
                        <strong>${formatRupees(item.modalPrice)}</strong><b>${escapeHtml(item.arrivalDate)}</b></div>`).join("")}`
                    : `<div class="app-empty">No mandi price records are stored yet.</div>`;
            }
        } else if (file === "crops.html") {
            document.querySelector(".crop-grid")?.remove();
            document.querySelectorAll(".crop-summary strong").forEach(value => value.textContent = "—");
            const panel = mountPanel(main, "app-my-crops", "Your live crop records", "Private to your farm account.", "Add a crop", "add-crop-record");
            const records = await request("/records?kind=CROP");
            panel.querySelector("[data-records]").innerHTML = records.length
                ? records.map(record => recordCard(record, { removable: true })).join("")
                : `<div class="app-empty">No crops added yet. Add your first crop to begin tracking it.</div>`;
            const count = document.querySelector(".crop-summary strong");
            if (count) count.textContent = String(records.length).padStart(2, "0");
            const totalArea = records.reduce((totals, item) => {
                const unit = item.unit || "Unspecified";
                totals[unit] = (totals[unit] || 0) + Number(item.quantity || 0);
                return totals;
            }, {});
            const areaText = Object.entries(totalArea).map(([unit, value]) => `${value.toLocaleString()} ${unit}`).join(", ") || "0";
            const summaryValues = document.querySelectorAll(".crop-summary > div");
            if (summaryValues[1]) summaryValues[1].querySelector("strong").textContent = areaText;
            if (summaryValues[2]) {
                summaryValues[2].querySelector("span").textContent = "Active crops";
                summaryValues[2].querySelector("strong").textContent = String(records.filter(item => item.status === "ACTIVE").length);
            }
            if (summaryValues[3]) {
                summaryValues[3].querySelector("span").textContent = "Other status";
                summaryValues[3].querySelector("strong").textContent = String(records.filter(item => item.status !== "ACTIVE").length);
            }
        } else if (file === "activities.html") {
            document.querySelector(".activity-filter")?.remove();
            document.querySelector(".activity-columns")?.remove();
            const overview = document.querySelector(".activity-overview");
            if (overview) overview.innerHTML = `<div class="app-empty">Loading activity records…</div>`;
            const panel = mountPanel(main, "app-my-activities", "Your activity tracker", "Plan field work and keep a record of completed tasks.", "Plan an activity", "add-activity");
            const records = await request("/records?kind=ACTIVITY");
            const completed = records.filter(item => item.status === "COMPLETED").length;
            if (overview) overview.innerHTML = `<div class="overview-main"><span>Farm Activity</span>
                <h2>${records.length ? `${Math.round(completed * 100 / records.length)}% completed` : "No activities recorded"}</h2>
                <p>Activity totals are based on records saved to your account.</p>
                <small>${completed} completed of ${records.length}</small></div>
                <div class="overview-number"><strong>${records.length}</strong><span>Total Activities</span></div>
                <div class="overview-number"><strong>${completed}</strong><span>Completed</span></div>`;
            panel.querySelector("[data-records]").innerHTML = records.length
                ? records.map(record => recordCard(record, { removable: true })).join("")
                : `<div class="app-empty">Your activity list is clear. Add a task when you’re ready.</div>`;
        } else if (file === "market.html") {
            await renderMarketPrices();
            const panel = mountPanel(main, "app-marketplace", "Active harvest listings", "Browse farmer listings currently available on the platform.", currentUser.role === "farmer" ? "List your harvest" : "", "add-listing");
            const listings = await request("/records/marketplace");
            panel.querySelector("[data-records]").innerHTML = listings.length
                ? listings.map(record => recordCard(record, { closable: record.ownerId === currentUser.id })).join("")
                : `<div class="app-empty">No fresh listings yet. Check back soon or list your harvest.</div>`;
        } else if (file === "settings.html") {
            const panel = mountPanel(main, "app-feedback-form", "Help us grow", "Send private product feedback to the platform team.", "Share feedback", "add-feedback");
            panel.querySelector("[data-records]").innerHTML = `<div class="app-empty">Your feedback helps us prioritize useful improvements for farmers and buyers.</div>`;
        }
        main.querySelectorAll("[data-action]").forEach(button => button.addEventListener("click", () => {
            if (button.dataset.action === "add-crop-record") openRecordDialog("CROP", "Add a crop to your farm");
            if (button.dataset.action === "add-activity") openRecordDialog("ACTIVITY", "Plan a farm activity");
            if (button.dataset.action === "add-listing") openRecordDialog("LISTING", "Create a harvest listing");
            if (button.dataset.action === "add-feedback") openRecordDialog("FEEDBACK", "Share platform feedback");
        }));
        const addCropButton = document.querySelector(".add-crop-btn");
        if (addCropButton && !addCropButton.dataset.wired) {
            addCropButton.dataset.wired = "true";
            addCropButton.addEventListener("click", () => openRecordDialog("CROP", "Add a crop to your farm"));
        }
        const addActivityButton = document.querySelector(".activity-add");
        if (addActivityButton && !addActivityButton.dataset.wired) {
            addActivityButton.dataset.wired = "true";
            addActivityButton.addEventListener("click", () => openRecordDialog("ACTIVITY", "Plan a farm activity"));
        }
        main.querySelectorAll("[data-delete-record]").forEach(button => button.addEventListener("click", async () => {
            try {
                await request(`/records/${button.dataset.deleteRecord}`, { method: "DELETE" });
                toast("Record removed.");
                await renderFarmRecords();
            } catch (error) { toast(error.message, "error"); }
        }));
        main.querySelectorAll("[data-close-listing]").forEach(button => button.addEventListener("click", async () => {
            try {
                await request(`/records/${button.dataset.closeListing}/close`, { method: "POST" });
                toast("Listing marked as sold.");
                await renderFarmRecords();
            } catch (error) { toast(error.message, "error"); }
        }));
        main.querySelectorAll("[data-complete-activity]").forEach(button => button.addEventListener("click", async () => {
            try {
                await request(`/records/${button.dataset.completeActivity}/status`, {
                    method: "PATCH", body: JSON.stringify({ status: "COMPLETED" })
                });
                toast("Activity marked complete.");
                await renderFarmRecords();
            } catch (error) { toast(error.message, "error"); }
        }));
    }

    function marketPriceQuery() {
        const params = new URLSearchParams();
        if (currentUser?.state) params.set("state", currentUser.state);
        if (currentUser?.district) params.set("district", currentUser.district);
        return params.toString();
    }

    function formatRupees(amount) {
        return `₹${Number(amount).toLocaleString("en-IN")}`;
    }

    async function renderMarketPrices() {
        const main = document.querySelector("main");
        const summary = main?.querySelector(".market-summary");
        const section = main?.querySelector(".market-section");
        if (!summary || !section) return;
        summary.innerHTML = `<div class="app-empty">Loading prices stored in the database…</div>`;
        const pendingTable = section.querySelector(".market-table");
        if (pendingTable) pendingTable.innerHTML = `<div class="app-empty">Loading provider records…</div>`;
        const query = marketPriceQuery();
        const prices = await request(`/market/prices${query ? `?${query}` : ""}`);
        const latestTime = prices.reduce((latest, item) => Math.max(latest, Date.parse(item.fetchedAt) || 0), 0);
        const latestItems = [...prices].sort((a, b) => (b.arrivalDate || "").localeCompare(a.arrivalDate || ""));
        const cardPrices = latestItems.slice(0, 4);
        summary.innerHTML = cardPrices.length ? cardPrices.map(item => `
            <div class="market-card">
                <span>${escapeHtml(item.commodity)} · ${escapeHtml(item.market)}</span>
                <strong>${formatRupees(item.modalPrice)}</strong>
                <small>per quintal · ${escapeHtml(item.arrivalDate)}</small>
                <em>${escapeHtml(item.state)}</em>
            </div>`).join("") : `<div class="app-empty">No mandi prices are available in the database for this location yet.</div>`;
        const title = section.querySelector(".section-heading h2");
        const subtitle = section.querySelector(".section-heading p");
        if (title) title.textContent = "Reported mandi prices";
        if (subtitle) subtitle.textContent = "AGMARKNET prices stored in the platform database; arrival date is shown per record.";
        section.querySelector(".section-heading select")?.remove();
        const table = section.querySelector(".market-table");
        if (table) {
            table.innerHTML = `<div class="table-head"><span>Crop</span><span>Market / location</span><span>Min price</span><span>Max price</span><span>Modal price</span></div>
                ${latestItems.length ? latestItems.map(item => `<div class="table-row">
                    <span>${escapeHtml(item.commodity)}</span>
                    <span>${escapeHtml([item.market, item.district, item.state].filter(Boolean).join(", "))}</span>
                    <b>${formatRupees(item.minPrice)}</b>
                    <b>${formatRupees(item.maxPrice)}</b>
                    <strong>${formatRupees(item.modalPrice)}</strong>
                </div>`).join("") : `<div class="app-empty">No stored market price records. An administrator can refresh from AGMARKNET.</div>`}`;
        }
        const updated = main.querySelector(".market-date");
        if (updated) updated.textContent = latestTime ? `Database refreshed ${new Date(latestTime).toLocaleString()}` : "No market data loaded";
        main.querySelector(".market-insight")?.remove();
    }

    async function renderAdminMarket() {
        if (!currentUser || file !== "admin-market.html") return;
        const page = document.querySelector(".market-page");
        if (!page) return;
        page.querySelectorAll(".market-filter, .market-stats, .market-grid, .market-insight").forEach(element => element.remove());
        const panel = mountPanel(page, "app-admin-market-prices", "AGMARKNET mandi prices",
            "Provider records are saved in the database. Refresh requires DATA_GOV_API_KEY on the backend.", "", "");
        const host = panel.querySelector("[data-records]");
        host.innerHTML = `<label>State<input data-market-state maxlength="80" value="${escapeHtml(currentUser.state || "")}" placeholder="Enter state"></label>
            <label>District<input data-market-district maxlength="80" value="${escapeHtml(currentUser.district || "")}" placeholder="Enter district"></label>
            <button class="app-button" type="button" data-refresh-market>Refresh provider data</button>
            <div data-market-results></div>`;
        const renderRows = async () => {
            const queryParams = new URLSearchParams();
            const state = host.querySelector("[data-market-state]").value.trim();
            const district = host.querySelector("[data-market-district]").value.trim();
            if (state) queryParams.set("state", state);
            if (district) queryParams.set("district", district);
            const query = queryParams.toString();
            const prices = await request(`/market/prices${query ? `?${query}` : ""}`);
            const latestFetch = prices.reduce((latest, item) => Math.max(latest, Date.parse(item.fetchedAt) || 0), 0);
            const lastUpdated = page.querySelector(".market-date strong");
            if (lastUpdated) lastUpdated.textContent = latestFetch ? new Date(latestFetch).toLocaleString() : "No database records";
            host.querySelector("[data-market-results]").innerHTML = prices.length ? `<div class="app-market-table">
                <div class="app-market-row"><strong>Commodity</strong><strong>Market</strong><strong>Arrival</strong><strong>Min / modal / max ₹</strong><strong>Fetched</strong></div>
                ${prices.map(item => `<div class="app-market-row"><span>${escapeHtml(item.commodity)}</span>
                    <span>${escapeHtml([item.market, item.district, item.state].filter(Boolean).join(", "))}</span>
                    <span>${escapeHtml(item.arrivalDate)}</span>
                    <span>${formatRupees(item.minPrice)} / ${formatRupees(item.modalPrice)} / ${formatRupees(item.maxPrice)}</span>
                    <span>${new Date(item.fetchedAt).toLocaleString()}</span></div>`).join("")}</div>`
                : `<div class="app-empty">No saved provider records match these filters.</div>`;
            return { state, district };
        };
        await renderRows();
        host.querySelector("[data-refresh-market]").addEventListener("click", async event => {
            const button = event.currentTarget;
            button.disabled = true;
            try {
                const filters = await renderRows();
                const params = new URLSearchParams();
                if (filters.state) params.set("state", filters.state);
                if (filters.district) params.set("district", filters.district);
                await request(`/market/prices/refresh${params.size ? `?${params}` : ""}`, { method: "POST" });
                toast("AGMARKNET records were refreshed and saved.");
                await renderRows();
            } catch (error) {
                toast(error.message, "error");
            } finally {
                button.disabled = false;
            }
        });
    }

    async function renderConsultations() {
        if (!currentUser || !["expert-consult.html", "previous-consultations.html", "expert-dashboard.html", "farmer-queries.html"].includes(file)) return;
        const main = document.querySelector("main");
        if (file === "expert-dashboard.html") {
            main?.querySelectorAll(".stats, .field-panel, .weather-panel, .queries-panel, .consultations-panel, .alerts-panel, .quick-panel")
                .forEach(element => element.remove());
        } else if (file === "expert-consult.html") {
            main?.querySelectorAll(".consultation-summary, .consultation-grid").forEach(element => element.remove());
        } else if (file === "previous-consultations.html") {
            main?.querySelectorAll(".previous-summary, .history-panel, .topic-panel").forEach(element => element.remove());
        } else if (file === "farmer-queries.html") {
            main?.querySelectorAll(".query-summary, .query-tools, .queries-list, .query-detail, .query-insight").forEach(element => element.remove());
        }
        const records = await request("/consultations");
        const panel = mountPanel(main, "app-consultations", "Live consultations", "Requests are private to the farmer and assigned expert, and read from the database.", "", "");
        panel.querySelector("[data-records]").innerHTML = records.length
            ? records.map(item => `<article class="app-record-card">
                <h3>${escapeHtml(item.topic)} · ${escapeHtml(item.crop)}</h3>
                <p>${escapeHtml(item.question)}</p>
                <div class="app-record-meta"><span>${escapeHtml(item.status)}</span><span>${new Date(item.createdAt).toLocaleString()}</span></div>
                ${currentUser.role === "expert" && item.status === "PENDING" ? `<button class="app-button" data-consult-status="${escapeHtml(item.id)}" data-status="ACCEPTED">Accept request</button>` : ""}
                ${currentUser.role === "expert" && item.status === "ACCEPTED" ? `<button class="app-button" data-consult-status="${escapeHtml(item.id)}" data-status="COMPLETED">Mark completed</button>` : ""}
            </article>`).join("")
            : `<div class="app-empty">No consultation requests yet.</div>`;
        panel.querySelectorAll("[data-consult-status]").forEach(button => button.addEventListener("click", async () => {
            try {
                await request(`/consultations/${button.dataset.consultStatus}/status`, {
                    method: "PATCH", body: JSON.stringify({ status: button.dataset.status })
                });
                toast("Consultation updated.");
                await renderConsultations();
            } catch (error) { toast(error.message, "error"); }
        }));
    }

    async function renderAdminUsers() {
        if (!currentUser || file !== "admin-users.html") return;
        document.querySelector(".user-sections")?.remove();
        const initialDirectory = document.querySelector(".directory-list");
        if (initialDirectory) initialDirectory.innerHTML = `<div class="app-empty">Loading accounts from the database…</div>`;
        document.querySelectorAll(".user-overview strong").forEach(value => value.textContent = "—");
        const users = await fetchAllAdminUsers();
        const counts = users.reduce((result, user) => {
            result[user.role] = (result[user.role] || 0) + 1;
            return result;
        }, {});
        document.querySelector(".overview-growth")?.remove();
        document.querySelector(".user-sections")?.remove();
        const overview = document.querySelector(".user-overview");
        if (overview) {
            const total = overview.querySelector(".overview-main strong");
            if (total) total.textContent = String(users.length);
            overview.querySelectorAll(".overview-item").forEach(item => {
                const role = item.querySelector("small")?.textContent.trim().toLowerCase().replace(/s$/, "");
                const count = item.querySelector("strong");
                if (count) count.textContent = String(counts[role] || 0);
            });
        }
        const directory = document.querySelector(".directory-list");
        if (directory) {
            directory.innerHTML = users.length ? users.map(user => {
                const name = `${user.firstName} ${user.lastName}`;
                return `<div class="directory-row">
                    <div class="directory-user"><div class="mini-avatar">${escapeHtml(name.split(/\s+/).map(part => part[0]).join("").slice(0, 2).toUpperCase())}</div>
                    <div><strong>${escapeHtml(name)}</strong><small>${escapeHtml(user.email)}</small></div></div>
                    <span class="role-pill">${escapeHtml(user.role)}</span>
                    <span class="joined-date">${new Date(user.createdAt).toLocaleDateString()}</span>
                    <span class="active-pill">${escapeHtml(user.accountStatus || "Active")}</span>
                </div>`;
            }).join("") : `<div class="app-empty">No user accounts yet.</div>`;
        }
        document.querySelector(".create-user-btn")?.addEventListener("click", () => window.location.assign("add-farmer.html"), { once: true });
    }

    async function renderAdminFeedback() {
        if (!currentUser || file !== "admin-feedback.html") return;
        const page = document.querySelector(".feedback-page");
        page?.querySelectorAll(".feedback-summary, .feedback-layout, .feedback-response").forEach(element => element.remove());
        const date = document.querySelector(".feedback-date strong");
        if (date) date.textContent = new Date().toLocaleString();
        const entries = await request("/records?kind=FEEDBACK");
        const panel = mountPanel(page || document.querySelector("main"), "app-feedback", "Community feedback", "Feedback submitted by signed-in users and stored in the database.", "", "");
        panel.querySelector("[data-records]").innerHTML = entries.length
            ? entries.map(entry => `<article class="app-record-card"><h3>${escapeHtml(entry.title)}</h3>
                <span class="app-status">${escapeHtml(entry.category || "Feedback")}</span>
                <p>${escapeHtml(entry.description)}</p>
                <div class="app-record-meta"><span>Submitted ${new Date(entry.createdAt).toLocaleString()}</span></div></article>`).join("")
            : `<div class="app-empty">No feedback has been submitted yet.</div>`;
    }

    async function renderAdminFarmers() {
        if (!currentUser || file !== "admin-farmers.html") return;
        const body = document.querySelector(".farmers-table tbody");
        if (body) body.innerHTML = `<tr><td colspan="7">Loading farmer accounts from the database…</td></tr>`;
        document.querySelectorAll(".summary-box strong").forEach(value => value.textContent = "—");
        const users = await fetchAllAdminUsers();
        const farmers = users.filter(user => user.role === "farmer");
        if (body) {
            const statusClass = { ACTIVE: "active-status", PENDING: "pending-status", INACTIVE: "inactive-status" };
            body.innerHTML = farmers.length ? farmers.map(user => {
                const name = `${user.firstName} ${user.lastName}`;
                const status = (user.accountStatus || "ACTIVE").toUpperCase();
                const location = [user.village, user.district, user.state].filter(Boolean).join(", ") || user.location || "—";
                const details = [
                    `Username: ${user.username || "—"}`,
                    `Email: ${user.email}`,
                    `Gender: ${user.gender || "—"}`,
                    `PIN: ${user.pinCode || "—"}`,
                    `Farming type: ${user.farmingType || "—"}`,
                    `Land: ${user.landArea == null ? "—" : `${user.landArea} ${user.landUnit || ""}`.trim()}`
                ].join(" · ");
                return `<tr data-farmer-row data-status="${escapeHtml(status)}" data-search="${escapeHtml(`${name} ${user.email} ${user.phone || ""} ${location} ${user.mainCrop || ""}`.toLowerCase())}">
                    <td><div class="farmer-person"><div class="farmer-avatar">${escapeHtml(name.split(/\s+/).map(part => part[0]).join("").slice(0, 2).toUpperCase())}</div>
                    <div><strong>${escapeHtml(name)}</strong><span>${escapeHtml(user.email)}</span></div></div></td>
                    <td>${escapeHtml(user.phone || "—")}</td><td>${escapeHtml(location)}</td><td>${escapeHtml(user.mainCrop || "—")}</td>
                    <td>${new Date(user.createdAt).toLocaleDateString()}</td>
                    <td><span class="farmer-status ${statusClass[status] || "inactive-status"}">${escapeHtml(status)}</span></td>
                    <td><button class="view-btn" type="button" data-farmer-details="${escapeHtml(user.id)}">Details</button></td></tr>
                    <tr data-farmer-detail-row="${escapeHtml(user.id)}" hidden><td colspan="7">${escapeHtml(details)}</td></tr>`;
            }).join("") : `<tr><td colspan="7">No farmer accounts have registered yet.</td></tr>`;

            const counts = farmers.reduce((total, farmer) => {
                const status = (farmer.accountStatus || "ACTIVE").toUpperCase();
                total[status] = (total[status] || 0) + 1;
                return total;
            }, {});
            document.querySelectorAll(".summary-box strong").forEach((value, index) => {
                value.textContent = String(index === 0 ? farmers.length : index === 1 ? counts.ACTIVE || 0
                    : index === 2 ? counts.PENDING || 0 : counts.INACTIVE || 0);
            });
            const search = document.querySelector(".farmer-search input");
            const filter = document.querySelector(".farmer-tools select");
            const applyFilters = () => {
                const query = (search?.value || "").trim().toLowerCase();
                const status = (filter?.value || "").toUpperCase();
                body.querySelectorAll("[data-farmer-row]").forEach(row => {
                    const visible = row.dataset.search.includes(query) && (!status || status === "ALL" || row.dataset.status === status);
                    row.hidden = !visible;
                    const detail = body.querySelector(`[data-farmer-detail-row="${row.querySelector("[data-farmer-details]")?.dataset.farmerDetails}"]`);
                    if (detail && !visible) detail.hidden = true;
                });
            };
            search?.addEventListener("input", applyFilters);
            filter?.addEventListener("change", applyFilters);
            filter?.querySelectorAll("option").forEach(option => {
                option.value = option.textContent.trim().toUpperCase() === "ALL STATUS" ? "ALL" : option.textContent.trim().toUpperCase();
            });
            body.querySelectorAll("[data-farmer-details]").forEach(button => button.addEventListener("click", () => {
                const details = body.querySelector(`[data-farmer-detail-row="${button.dataset.farmerDetails}"]`);
                if (details) details.hidden = !details.hidden;
            }));
        }
    }

    function wireAddFarmerForm() {
        if (file !== "add-farmer.html") return;
        const form = document.querySelector(".farmer-form");
        if (!form) return;
        form.addEventListener("submit", async event => {
            event.preventDefault();
            const button = form.querySelector('[type="submit"]');
            button.disabled = true;
            const values = Object.fromEntries(new FormData(form).entries());
            if (values.landArea === "") values.landArea = null;
            else if (values.landArea != null) values.landArea = Number(values.landArea);
            try {
                await request("/admin/farmers", { method: "POST", body: JSON.stringify(values) });
                form.reset();
                toast("Farmer account created successfully.");
            } catch (error) {
                toast(error.message, "error");
            } finally {
                button.disabled = false;
            }
        });
    }

    async function renderAdminListings() {
        if (!currentUser || file !== "admin-market.html") return;
        const listings = await request("/records?kind=LISTING");
        const panel = mountPanel(document.querySelector("main"), "app-admin-listings", "Live harvest listings", "Listings currently stored in the marketplace.", "", "");
        panel.querySelector("[data-records]").innerHTML = listings.length
            ? listings.map(record => recordCard(record, { closable: record.status === "ACTIVE", removable: true })).join("")
            : `<div class="app-empty">No listings have been published yet.</div>`;
        panel.querySelectorAll("[data-close-listing]").forEach(button => button.addEventListener("click", async () => {
            try {
                await request(`/records/${button.dataset.closeListing}/close`, { method: "POST" });
                toast("Listing closed.");
                await renderAdminListings();
            } catch (error) { toast(error.message, "error"); }
        }));
        panel.querySelectorAll("[data-delete-record]").forEach(button => button.addEventListener("click", async () => {
            try {
                await request(`/records/${button.dataset.deleteRecord}`, { method: "DELETE" });
                toast("Listing removed.");
                await renderAdminListings();
            } catch (error) { toast(error.message, "error"); }
        }));
    }

    async function renderAdminDashboard() {
        if (!currentUser || file !== "admin-dashboard.html") return;
        document.querySelectorAll(".content-grid, .second-grid").forEach(element => element.remove());
        document.querySelectorAll(".stats-row .stat-card strong").forEach(value => value.textContent = "—");
        const summary = await request("/admin/summary");
        const date = document.querySelector(".dashboard-date strong");
        if (date) date.textContent = new Date().toLocaleDateString(undefined, { dateStyle: "long" });
        const values = {
            "Total Farmers": summary.farmers,
            "Total Crops": summary.crops,
            "Total Area (Acres)": summary.activities,
            "Expert Queries": summary.consultations
        };
        document.querySelectorAll(".stats-row .stat-card").forEach(card => {
            const label = card.querySelector(".stat-info span")?.textContent.trim();
            const value = card.querySelector(".stat-info strong");
            if (label === "Total Area (Acres)" && card.querySelector(".stat-info span")) {
                card.querySelector(".stat-info span").textContent = "Farm Activities";
            }
            if (value && values[label] != null) value.textContent = String(values[label]);
        });
        const farmers = await request("/admin/users?size=8&sort=createdAt,desc");
        const panel = mountPanel(document.querySelector(".admin-content"), "app-admin-recent-farmers",
            "Recent farmer accounts", "Most recently created farmer accounts from the database.", "", "");
        const recent = (farmers.content || []).filter(user => user.role === "farmer");
        panel.querySelector("[data-records]").innerHTML = recent.length ? recent.map(user => `
            <article class="app-record-card"><h3>${escapeHtml(user.firstName)} ${escapeHtml(user.lastName)}</h3>
            <p>${escapeHtml(user.email)}</p><div class="app-record-meta"><span>${escapeHtml(user.state || user.location || "Location not set")}</span>
            <span>${new Date(user.createdAt).toLocaleDateString()}</span></div></article>`).join("")
            : `<div class="app-empty">No farmer accounts have been created.</div>`;
    }

    async function renderAdminReports() {
        if (!currentUser || file !== "admin-reports.html") return;
        const summary = await request("/admin/summary");
        const values = [summary.farmers + summary.buyers + summary.experts + summary.admins, summary.crops,
            summary.consultations, summary.feedback];
        document.querySelectorAll(".report-stats .report-stat-card").forEach((card, index) => {
            const label = card.querySelector("span")?.textContent.trim();
            const value = card.querySelector("strong");
            if (!value) return;
            const labels = ["Platform accounts", "Crop records", "Expert consultations", "User feedback"];
            const labelNode = card.querySelector("span");
            if (labelNode) labelNode.textContent = labels[index];
            value.textContent = String(values[index] ?? 0);
        });
        document.querySelectorAll(".generator-card button").forEach(button => {
            button.addEventListener("click", async () => {
                const title = button.closest(".generator-card")?.querySelector(".generator-content strong")?.textContent || "Platform report";
                try {
                    let rows;
                    if (title === "Farmer Report") {
                        const users = await fetchAllAdminUsers();
                        rows = [["First name", "Last name", "Email", "Phone", "Role", "Created at"],
                            ...users.filter(user => user.role === "farmer").map(user =>
                                [user.firstName, user.lastName, user.email, user.phone, user.role, user.createdAt])];
                    } else if (title === "Crop Report") {
                        const crops = await request("/crops/admin");
                        rows = [["Name", "Type", "Season", "Variety", "Average price", "Status"],
                            ...crops.map(crop => [crop.name, crop.type, crop.season, crop.variety, crop.averagePrice, crop.status])];
                    } else if (title === "Market Report") {
                        const listings = await request("/records?kind=LISTING");
                        rows = [["Listing", "Quantity", "Unit", "Price", "Location", "Status"],
                            ...listings.map(record => [record.title, record.quantity, record.unit, record.price, record.location, record.status])];
                    } else {
                        const consultations = await request("/consultations");
                        rows = [["Topic", "Crop", "Question", "Status", "Created at"],
                            ...consultations.map(item => [item.topic, item.crop, item.question, item.status, item.createdAt])];
                    }
                    const csv = rows.map(row => row.map(value => `"${String(value ?? "").replace(/"/g, '""')}"`).join(",")).join("\r\n");
                    const link = document.createElement("a");
                    link.href = URL.createObjectURL(new Blob([csv], { type: "text/csv;charset=utf-8" }));
                    link.download = `${title.toLowerCase().replace(/[^a-z0-9]+/g, "-")}.csv`;
                    link.click();
                    window.setTimeout(() => URL.revokeObjectURL(link.href), 1000);
                    toast("Report downloaded.");
                } catch (error) { toast(error.message, "error"); }
            });
        });
    }

    async function renderPublicSummary() {
        if (file !== "index.html") return;
        const values = document.querySelectorAll(".about-box .about-stat strong");
        values.forEach(value => value.textContent = "—");
        const summary = await request("/public/summary");
        if (values[0]) values[0].textContent = Number(summary.farmers).toLocaleString();
        if (values[1]) values[1].textContent = Number(summary.activeCrops).toLocaleString();
        if (values[2]) values[2].textContent = Number(summary.activeListings).toLocaleString();
    }

    async function renderAdminCrops() {
        if (!currentUser || file !== "admin-crops.html") return;
        const tbody = document.querySelector(".crop-table tbody");
        if (!tbody) return;
        tbody.innerHTML = `<tr><td colspan="7">Loading crop records from the database…</td></tr>`;
        const crops = await request("/crops/admin");
        tbody.innerHTML = crops.length ? crops.map(crop => `<tr data-crop-row>
            <td><div class="crop-person"><div class="crop-image">🌱</div><div><strong>${escapeHtml(crop.name)}</strong>
            <span>${escapeHtml(crop.variety || crop.type)}</span></div></div></td>
            <td><span class="season">${escapeHtml(crop.season)}</span></td>
            <td>—</td><td>—</td>
            <td>${crop.averagePrice == null ? "—" : `₹${Number(crop.averagePrice).toLocaleString("en-IN")} / ${escapeHtml(crop.priceUnit || "unit")}`}</td>
            <td><span class="crop-status crop-active">${escapeHtml(crop.status)}</span></td>
            <td><button class="crop-view" data-delete-catalog-crop="${escapeHtml(crop.id)}">Remove</button></td>
            </tr>`).join("") : `<tr><td colspan="7">No catalog crops yet. Add the first one to build your reference library.</td></tr>`;
        const search = document.querySelector(".crop-search input");
        if (search) search.oninput = () => tbody.querySelectorAll("[data-crop-row]").forEach(row => {
            row.hidden = !row.textContent.toLowerCase().includes(search.value.toLowerCase());
        });
        search?.dispatchEvent(new Event("input"));
        tbody.querySelectorAll("[data-delete-catalog-crop]").forEach(button => button.addEventListener("click", async () => {
            if (!window.confirm("Remove this crop from the catalog?")) return;
            try {
                await request(`/crops/${button.dataset.deleteCatalogCrop}`, { method: "DELETE" });
                toast("Crop removed from the catalog.");
                await renderAdminCrops();
            } catch (error) { toast(error.message, "error"); }
        }));
    }

    function populateSettings() {
        if (!currentUser || file !== "settings.html") return;
        const form = document.getElementById("profileSettingsForm");
        const field = name => form?.elements.namedItem(name);
        if (!form) return;
        if (field("fullName")) field("fullName").value = `${currentUser.firstName} ${currentUser.lastName}`;
        if (field("email")) field("email").value = currentUser.email;
        ["phone", "location", "state", "district", "farmName", "farmArea", "soilType", "irrigationMethod"].forEach(name => {
            const input = field(name);
            if (input) input.value = currentUser[name] || "";
        });
        const notificationNames = ["notifyWeather", "notifyMarket", "notifyActivities"];
        notificationNames.forEach(name => {
            const checkbox = field(name);
            if (checkbox) checkbox.checked = currentUser[name] ?? true;
        });
        const save = document.getElementById("saveSettings");
        const saveStatus = document.getElementById("settingsSaveStatus");
        form.addEventListener("input", () => {
            if (saveStatus) saveStatus.textContent = "You have unsaved changes.";
        });
        form.addEventListener("change", () => {
            if (saveStatus) saveStatus.textContent = "You have unsaved changes.";
        });
        form.addEventListener("submit", async event => {
            event.preventDefault();
            const [firstName, ...lastParts] = (field("fullName")?.value || "").trim().split(/\s+/);
            if (!firstName || !lastParts.length) {
                toast("Enter both your first and last name.", "error");
                return;
            }
            if (save) save.disabled = true;
            if (saveStatus) saveStatus.textContent = "Saving your changes…";
            try {
                currentUser = await request("/auth/me", {
                    method: "PATCH",
                    body: JSON.stringify({
                        firstName,
                        lastName: lastParts.join(" "),
                        phone: field("phone")?.value.trim() || "",
                        location: field("location")?.value.trim() || "",
                        state: field("state")?.value.trim() || "",
                        district: field("district")?.value.trim() || "",
                        farmName: field("farmName")?.value.trim() || "",
                        farmArea: field("farmArea")?.value.trim() || "",
                        soilType: field("soilType")?.value || "",
                        irrigationMethod: field("irrigationMethod")?.value || "",
                        notifyWeather: field("notifyWeather")?.checked ?? true,
                        notifyMarket: field("notifyMarket")?.checked ?? true,
                        notifyActivities: field("notifyActivities")?.checked ?? true
                    })
                });
                document.querySelectorAll(".user-info strong, .account-info strong, .admin-user-name").forEach(element => {
                    element.textContent = `${currentUser.firstName} ${currentUser.lastName}`;
                });
                if (saveStatus) saveStatus.textContent = "Saved to your account just now.";
                toast("Your profile has been updated.");
            } catch (error) {
                if (saveStatus) saveStatus.textContent = "Could not save your changes.";
                toast(error.message, "error");
            } finally {
                if (save) save.disabled = false;
            }
        });
    }

    function populateAdminProfile() {
        if (!currentUser || file !== "admin-profile.html") return;
        const form = document.getElementById("profileForm");
        const fields = [...(form?.querySelectorAll(".profile-field input") || [])];
        const status = form?.querySelector(".profile-field select");
        const name = `${currentUser.firstName} ${currentUser.lastName}`;
        if (fields[0]) fields[0].value = name;
        if (fields[1]) fields[1].value = currentUser.username || "—";
        if (fields[2]) fields[2].value = currentUser.email;
        if (fields[3]) fields[3].value = currentUser.phone || "";
        if (fields[4]) fields[4].value = currentUser.role;
        if (fields[5]) fields[5].value = currentUser.location || "";
        if (status) status.value = currentUser.accountStatus || "ACTIVE";
        const heading = document.querySelector(".profile-main h2");
        if (heading) heading.textContent = name;
        const role = document.querySelector(".profile-main > p");
        if (role) role.textContent = currentUser.role;
        const profileStatus = document.querySelector(".profile-status");
        if (profileStatus) profileStatus.innerHTML = `<span></span>${escapeHtml(currentUser.accountStatus || "ACTIVE")} Account`;
        const avatar = document.querySelector(".profile-avatar");
        if (avatar) avatar.textContent = name.split(/\s+/).map(part => part[0]).join("").slice(0, 2).toUpperCase();
        const side = document.querySelectorAll(".profile-side-info .side-info-row strong");
        if (side[0]) side[0].textContent = currentUser.email;
        if (side[1]) side[1].textContent = currentUser.role;
        if (side[2]) side[2].textContent = new Date(currentUser.createdAt).toLocaleDateString();
        if (side[3]) side[3].textContent = currentUser.accountStatus || "ACTIVE";
        const editable = [fields[0], fields[3], fields[5]].filter(Boolean);
        const editButton = document.getElementById("editProfileBtn");
        const saveButton = document.getElementById("saveProfileBtn");
        if (editButton) editButton.hidden = false;
        if (saveButton) saveButton.hidden = true;
        editButton?.addEventListener("click", () => {
            editable.forEach(field => { field.disabled = false; });
            editButton.hidden = true;
            if (saveButton) saveButton.hidden = false;
            editable[0]?.focus();
        });
        form?.addEventListener("submit", async event => {
            event.preventDefault();
            const [firstName, ...lastParts] = (fields[0]?.value || "").trim().split(/\s+/);
            if (!firstName || !lastParts.length) {
                toast("Enter both your first and last name.", "error");
                return;
            }
            if (saveButton) saveButton.disabled = true;
            try {
                currentUser = await request("/auth/me", {
                    method: "PATCH",
                    body: JSON.stringify({
                        firstName, lastName: lastParts.join(" "), phone: fields[3]?.value || "",
                        location: fields[5]?.value || ""
                    })
                });
                const updatedName = `${currentUser.firstName} ${currentUser.lastName}`;
                if (fields[0]) fields[0].value = updatedName;
                if (fields[3]) fields[3].value = currentUser.phone || "";
                if (fields[5]) fields[5].value = currentUser.location || "";
                if (heading) heading.textContent = updatedName;
                if (side[0]) side[0].textContent = currentUser.email;
                if (editable[0]) editable[0].disabled = true;
                if (editable[1]) editable[1].disabled = true;
                if (editable[2]) editable[2].disabled = true;
                if (editButton) editButton.hidden = false;
                if (saveButton) saveButton.hidden = true;
                toast("Profile changes saved to your account.");
            } catch (error) {
                toast(error.message, "error");
            } finally {
                if (saveButton) saveButton.disabled = false;
            }
        });
    }

    function populateExpertProfile() {
        if (!currentUser || file !== "expert-profile.html") return;
        const page = document.querySelector(".profile-content");
        page?.querySelectorAll(".details-area .profile-section:not(:first-child)").forEach(section => section.remove());
        const name = `${currentUser.firstName} ${currentUser.lastName}`;
        const nameNodes = document.querySelectorAll(".profile-info strong, .profile-main h3");
        nameNodes.forEach(node => { node.textContent = name; });
        const roleNodes = document.querySelectorAll(".profile-info span, .expert-role");
        roleNodes.forEach(node => { node.textContent = currentUser.role; });
        const avatar = document.querySelector(".large-avatar");
        if (avatar) avatar.textContent = name.split(/\s+/).map(part => part[0]).join("").slice(0, 2).toUpperCase();
        const status = document.querySelector(".profile-main .status");
        if (status) status.innerHTML = `<i></i>${escapeHtml(currentUser.accountStatus || "ACTIVE")} Account`;
        const contacts = document.querySelectorAll(".profile-contact strong");
        if (contacts[0]) contacts[0].textContent = currentUser.email;
        if (contacts[1]) contacts[1].textContent = currentUser.phone || "—";
        if (contacts[2]) contacts[2].textContent = currentUser.location || "—";
        const display = document.querySelectorAll(".view-form .display-field");
        if (display[0]) display[0].textContent = name;
        if (display[1]) display[1].textContent = currentUser.username || "—";
        if (display[2]) display[2].textContent = currentUser.email;
        if (display[3]) display[3].textContent = currentUser.phone || "—";
        if (display[4]) display[4].textContent = currentUser.role;
        display[5]?.closest(".form-group")?.remove();
        const inputs = [...document.querySelectorAll(".edit-form input")];
        if (inputs[0]) inputs[0].value = name;
        if (inputs[1]) inputs[1].value = currentUser.username || "—";
        if (inputs[2]) inputs[2].value = currentUser.email;
        if (inputs[3]) inputs[3].value = currentUser.phone || "";
        if (inputs[4]) inputs[4].value = currentUser.role;
        inputs[5]?.closest(".form-group")?.remove();
        [inputs[1], inputs[2], inputs[4]].forEach(input => { if (input) input.disabled = true; });
        if (inputs[0]) inputs[0].disabled = true;
        if (inputs[3]) inputs[3].disabled = true;
        const toggle = document.getElementById("edit-profile");
        toggle?.addEventListener("change", () => {
            if (inputs[0]) inputs[0].disabled = !toggle.checked;
            if (inputs[3]) inputs[3].disabled = !toggle.checked;
        });
        const save = document.querySelector(".edit-form .save-btn");
        save?.addEventListener("click", async () => {
            const [firstName, ...lastParts] = (inputs[0]?.value || "").trim().split(/\s+/);
            if (!firstName || !lastParts.length) {
                toast("Enter both your first and last name.", "error");
                return;
            }
            save.disabled = true;
            try {
                currentUser = await request("/auth/me", {
                    method: "PATCH",
                    body: JSON.stringify({
                        firstName, lastName: lastParts.join(" "), phone: inputs[3]?.value || ""
                    })
                });
                if (inputs[0]) inputs[0].value = `${currentUser.firstName} ${currentUser.lastName}`;
                if (display[0]) display[0].textContent = inputs[0]?.value || "";
                if (display[3]) display[3].textContent = currentUser.phone || "—";
                nameNodes.forEach(node => { node.textContent = `${currentUser.firstName} ${currentUser.lastName}`; });
                if (contacts[1]) contacts[1].textContent = currentUser.phone || "—";
                if (avatar) avatar.textContent = `${currentUser.firstName[0]}${currentUser.lastName[0]}`.toUpperCase();
                if (toggle) toggle.checked = false;
                if (inputs[0]) inputs[0].disabled = true;
                if (inputs[3]) inputs[3].disabled = true;
                toast("Expert profile saved to the database.");
            } catch (error) {
                toast(error.message, "error");
            } finally {
                save.disabled = false;
            }
        });
    }

    async function populateAdminSettings() {
        if (!currentUser || file !== "admin-settings.html") return;
        const result = await request("/admin/settings");
        const general = document.querySelector("#general .settings-form");
        const fields = general?.querySelectorAll("input");
        const selects = general?.querySelectorAll("select");
        if (fields?.[0]) fields[0].value = result.platform_name || "";
        if (fields?.[1]) fields[1].value = currentUser.email;
        if (selects?.[0]) selects[0].value = result.default_language || "";
        if (selects?.[1]) selects[1].value = result.time_zone || "";
        const accountFields = document.querySelectorAll("#account .settings-form input");
        if (accountFields[0]) accountFields[0].value = `${currentUser.firstName} ${currentUser.lastName}`;
        if (accountFields[1]) accountFields[1].value = currentUser.email;
        const accountName = document.querySelector(".admin-profile-box strong");
        if (accountName) accountName.textContent = `${currentUser.firstName} ${currentUser.lastName}`;
        const accountRole = document.querySelector(".admin-profile-box span");
        if (accountRole) accountRole.textContent = currentUser.role;
        const edit = document.querySelector(".edit-profile");
        edit?.addEventListener("click", () => window.location.assign("admin-profile.html"), { once: true });
        document.querySelector("#security")?.remove();
        document.querySelector('.settings-menu-item[href="#security"]')?.remove();
        const notificationKeys = ["notify_new_farmer", "notify_feedback", "notify_market_prices"];
        const notifications = document.querySelectorAll(".notification-setting input[type='checkbox']");
        notifications.forEach((checkbox, index) => {
            checkbox.checked = result[notificationKeys[index]] === "true";
        });
        const save = document.querySelector("#general .settings-save button");
        save?.addEventListener("click", async () => {
            save.disabled = true;
            try {
                const updated = await request("/admin/settings", {
                    method: "PUT",
                    body: JSON.stringify({
                        platformName: fields?.[0]?.value || "",
                        defaultLanguage: selects?.[0]?.value || "",
                        timeZone: selects?.[1]?.value || "",
                        notifyNewFarmer: notifications[0]?.checked ?? false,
                        notifyFeedback: notifications[1]?.checked ?? false,
                        notifyMarketPrices: notifications[2]?.checked ?? false
                    })
                });
                notifications.forEach((checkbox, index) => { checkbox.checked = updated[notificationKeys[index]] === "true"; });
                toast("Platform settings saved to the database.");
            } catch (error) {
                toast(error.message, "error");
            } finally {
                save.disabled = false;
            }
        }, { once: true });
    }

    function wireAdminCropForm() {
        const form = document.querySelector(".crop-form");
        if (!form || file !== "add-crop.html") return;
        form.addEventListener("submit", async event => {
            event.preventDefault();
            const fields = [...form.querySelectorAll("input, select, textarea")];
            const data = {
                name: fields[0].value,
                type: fields[1].value,
                season: fields[2].value,
                variety: fields[3].value,
                growingDuration: fields[4].value,
                waterRequirement: fields[5].value,
                soilType: fields[6].value,
                temperatureRange: fields[7].value,
                description: fields[8].value,
                averagePrice: fields[9].value ? Number(fields[9].value) : null,
                priceUnit: fields[10].value,
                msp: fields[11].value ? Number(fields[11].value) : null,
                status: fields[12].value
            };
            const button = form.querySelector('[type="submit"]');
            button.disabled = true;
            try {
                await request("/crops", { method: "POST", body: JSON.stringify(data) });
                toast("Crop added to the reference catalog.");
                window.location.assign("admin-crops.html");
            } catch (error) {
                toast(error.message, "error");
                button.disabled = false;
            }
        });
    }

    async function renderWeather() {
        if (file !== "weather.html") return;
        const main = document.querySelector("main");
        if (!main) return;
        const panel = mountPanel(main, "app-live-weather", "Live local forecast", "Powered by Open-Meteo. Forecast is guidance, not a substitute for local advisories.", "Refresh location", "refresh-live-weather");
        panel.querySelector("[data-action='refresh-live-weather']")?.addEventListener("click", () => renderWeather(), { once: true });
        const host = panel.querySelector("[data-records]");
        const current = document.querySelector(".current-weather");
        const week = document.querySelector(".week-weather");
        const locationBadge = document.querySelector(".weather-location");
        if (locationBadge) locationBadge.textContent = "📍 Requesting device location…";
        if (current) current.innerHTML = `<div class="app-empty">Requesting your device location for current weather…</div>`;
        if (week) week.innerHTML = "";
        document.querySelector(".hourly-weather")?.remove();
        document.querySelectorAll(".weather-section-title")[0]?.remove();
        host.innerHTML = `<div class="app-empty">Waiting for location permission…</div>`;
        try {
            let coords;
            let isLiveLocation = true;
            try {
                if (!navigator.geolocation) throw new Error("This browser does not provide device geolocation.");
                coords = await new Promise((resolve, reject) => navigator.geolocation.getCurrentPosition(
                    position => resolve({ latitude: position.coords.latitude, longitude: position.coords.longitude }),
                    reject,
                    { enableHighAccuracy: true, maximumAge: 300000, timeout: 15000 }));
            } catch (locationError) {
                if (currentUser?.latitude != null && currentUser?.longitude != null) {
                    coords = { latitude: currentUser.latitude, longitude: currentUser.longitude };
                    isLiveLocation = false;
                } else {
                    throw new Error("Device location is unavailable or permission was denied. Allow location access and reload this page.");
                }
            }
            if (isLiveLocation && currentUser) {
                currentUser = await request("/auth/me/location", {
                    method: "PATCH",
                    body: JSON.stringify({
                        latitude: coords.latitude,
                        longitude: coords.longitude,
                        location: `${Number(coords.latitude).toFixed(5)}, ${Number(coords.longitude).toFixed(5)}`,
                        state: currentUser.state || null,
                        district: currentUser.district || null,
                        village: currentUser.village || null
                    })
                });
            }
            let snapshot;
            try {
                snapshot = await request("/weather/refresh", { method: "POST" });
            } catch (refreshError) {
                try {
                    snapshot = await request("/weather/current");
                    coords = { latitude: snapshot.latitude, longitude: snapshot.longitude };
                    isLiveLocation = false;
                    toast(`Showing the last saved forecast; live refresh failed: ${refreshError.message}`, "error");
                } catch {
                    throw refreshError;
                }
            }
            const data = snapshot.forecast;
            const descriptions = { 0: "Clear", 1: "Mostly clear", 2: "Partly cloudy", 3: "Overcast", 45: "Fog", 48: "Freezing fog", 51: "Light drizzle", 53: "Drizzle", 55: "Dense drizzle", 56: "Freezing drizzle", 57: "Dense freezing drizzle", 61: "Light rain", 63: "Rain", 65: "Heavy rain", 66: "Freezing rain", 67: "Heavy freezing rain", 71: "Light snow", 73: "Snow", 75: "Heavy snow", 77: "Snow grains", 80: "Showers", 81: "Rain showers", 82: "Heavy showers", 85: "Snow showers", 86: "Heavy snow showers", 95: "Thunderstorm", 96: "Thunderstorm with hail", 99: "Severe thunderstorm with hail" };
            const weatherIcon = code => {
                const icon = code === 0 ? "sun"
                    : code === 1 || code === 2 ? "partly-cloudy"
                        : code === 45 || code === 48 ? "fog"
                            : code >= 71 && code <= 86 ? "snow"
                                : code >= 95 ? "storm"
                                    : code >= 51 ? "rain" : "cloud";
                const drawings = {
                    sun: `<circle cx="12" cy="12" r="4" /><path d="M12 2v2m0 16v2M4.93 4.93l1.42 1.42m11.3 11.3 1.42 1.42M2 12h2m16 0h2M4.93 19.07l1.42-1.42m11.3-11.3 1.42-1.42" />`,
                    "partly-cloudy": `<path d="M12 3v2m-6.36.64 1.42 1.42M4 12h2m.22 5.78 1.42-1.42M12 7a5 5 0 0 1 4.9 4" /><path d="M8 19h10a4 4 0 0 0 .4-8A6 6 0 0 0 7 10.5 4.3 4.3 0 0 0 8 19Z" />`,
                    cloud: `<path d="M7 19h10a4 4 0 0 0 .4-8A6 6 0 0 0 6 10.5 4.3 4.3 0 0 0 7 19Z" />`,
                    fog: `<path d="M7 14h10a4 4 0 0 0 .4-8A6 6 0 0 0 6 5.5 4.3 4.3 0 0 0 7 14Zm-3 3h16m-14 3h12" />`,
                    rain: `<path d="M7 14h10a4 4 0 0 0 .4-8A6 6 0 0 0 6 5.5 4.3 4.3 0 0 0 7 14Z" /><path d="m8 17-1 3m6-3-1 3m6-3-1 3" />`,
                    snow: `<path d="M7 13h10a4 4 0 0 0 .4-8A6 6 0 0 0 6 4.5 4.3 4.3 0 0 0 7 13Z" /><path d="M8 17v4m-2-2h4m5-2v4m-2-2h4" />`,
                    storm: `<path d="M7 13h10a4 4 0 0 0 .4-8A6 6 0 0 0 6 4.5 4.3 4.3 0 0 0 7 13Z" /><path d="m13 14-3 5h3l-1 3 4-6h-3l1-2" />`
                };
                return `<svg class="weather-condition-icon weather-condition-icon-${icon}" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${drawings[icon]}</svg>`;
            };
            const daily = data.daily.time.map((date, i) => `<div class="app-weather-day">
                <strong>${new Date(`${date}T12:00:00`).toLocaleDateString(undefined, { weekday: "short" })}</strong>
                <span class="app-weather-description">${weatherIcon(data.daily.weather_code[i])}${escapeHtml(descriptions[data.daily.weather_code[i]] || "Variable")}</span>
                <span>${Math.round(data.daily.temperature_2m_max[i])}° / ${Math.round(data.daily.temperature_2m_min[i])}°C</span>
                <small>Rain ${data.daily.precipitation_probability_max[i] ?? 0}%</small>
            </div>`).join("");
            const placeName = `${Number(coords.latitude).toFixed(5)}, ${Number(coords.longitude).toFixed(5)}`;
            host.innerHTML = `<div class="app-record-card"><h3>${escapeHtml(placeName)}</h3>
                <div class="app-record-meta"><span>${Math.round(data.current.temperature_2m)}°C · ${escapeHtml(descriptions[data.current.weather_code] || "Variable")}</span>
                <span>Feels like ${Math.round(data.current.apparent_temperature)}°C</span>
                <span>Humidity ${data.current.relative_humidity_2m}%</span>
                <span>Wind ${Math.round(data.current.wind_speed_10m)} km/h</span>
                <span>Rain ${data.current.precipitation} mm</span>
                <span>Stored ${new Date(snapshot.fetchedAt).toLocaleString()}</span></div></div>
                <div class="app-weather-grid">${daily}</div>`;
            if (locationBadge) locationBadge.textContent = `📍 ${isLiveLocation ? "Live GPS" : "Saved location"} · ${escapeHtml(placeName)}`;
            if (current) current.innerHTML = `<div class="temperature"><span>${isLiveLocation ? "Live device location" : "Saved device location"}</span>
                <h2>${Math.round(data.current.temperature_2m)}°<small>C</small></h2>
                <h3>${weatherIcon(data.current.weather_code)}${escapeHtml(descriptions[data.current.weather_code] || "Variable")}</h3>
                <p>Feels like ${Math.round(data.current.apparent_temperature)}°C</p></div>
                <div class="weather-stats"><div><b>💧 ${data.current.relative_humidity_2m}%</b><span>Humidity</span></div>
                <div><b>💨 ${Math.round(data.current.wind_speed_10m)} km/h</b><span>Wind</span></div>
                <div><b>🌧️ ${data.current.precipitation} mm</b><span>Current rain</span>                </div></div>`;
                if (week) week.innerHTML = daily;
        } catch (error) {
            host.innerHTML = `<div class="app-empty">${escapeHtml(error.message)}</div>`;
            if (current) current.innerHTML = `<div class="app-empty">${escapeHtml(error.message)}</div>`;
            if (week) week.innerHTML = "";
            if (locationBadge) locationBadge.textContent = "Location unavailable";
        }
    }

    function enableExpertQuestion() {
        if (!["expert.html", "crop-problems.html"].includes(file)) return;
        document.querySelectorAll(".expert-profile button:not(.disabled-btn)").forEach(button => {
            button.removeAttribute("onclick");
            button.addEventListener("click", () => window.openQuestion?.());
        });
        window.submitQuestion = async () => {
            const modal = document.getElementById("questionModal");
            if (!modal) return;
            const controls = modal.querySelectorAll("select, input, textarea");
            const category = controls[0]?.value?.trim();
            const crop = controls[1]?.value?.trim();
            const question = controls[2]?.value?.trim();
            if (!category || !crop || !question) {
                toast("Choose a topic, enter the crop, and describe the problem.", "error");
                return;
            }
            try {
                await request("/consultations", {
                    method: "POST",
                    body: JSON.stringify({ topic: category, crop, question })
                });
                toast("Your question has been sent to the expert team.");
                modal.style.display = "none";
            } catch (error) {
                toast(error.status === 401 || error.status === 403
                    ? "Sign in with a farmer account to submit a consultation request."
                    : error.message, "error");
            }
        };
    }

    async function start() {
        if (!document.querySelector('link[data-app-styles]')) {
            const styles = document.createElement("link");
            styles.rel = "stylesheet";
            styles.href = "app.css";
            styles.dataset.appStyles = "true";
            document.head.append(styles);
        }
        document.querySelectorAll('a[href="#"]').forEach(link => link.addEventListener("click", event => {
            if (link.closest(".crop-bottom")) {
                event.preventDefault();
                toast("Crop details will appear here when you add a crop to your farm.");
            }
        }));
        enableExpertQuestion();
        wireAdminCropForm();
        wireAddFarmerForm();
        try {
            await renderPublicSummary();
        } catch (error) {
            if (file === "index.html") toast(error.message, "error");
        }

        if (pageRoles[file]) {
            try {
                currentUser = await request("/auth/me");
            } catch {
                window.location.replace(`login.html?return=${encodeURIComponent(file)}`);
                return;
            }
            if (!pageRoles[file].includes(currentUser.role)) {
                toast("That page isn’t available for this account. Opening your dashboard.");
                window.location.replace(roleLanding[currentUser.role] || "login.html");
                return;
            }
            await renderWeather();
            document.querySelectorAll(".user-info strong, .account-info strong, .admin-user-name").forEach(element => {
                element.textContent = `${currentUser.firstName} ${currentUser.lastName}`;
            });
            document.querySelectorAll(".user-info small").forEach(element => {
                element.textContent = `${currentUser.role[0].toUpperCase()}${currentUser.role.slice(1)} account`;
            });
            const welcome = document.querySelector(".dashboard-header h1");
            if (welcome) welcome.textContent = `Welcome back, ${currentUser.firstName}!`;
            document.querySelectorAll("a.logout, a[href='login.html'], a[href='../login.html']").forEach(link => {
                link.addEventListener("click", async event => {
                    event.preventDefault();
                    try { await request("/auth/logout", { method: "POST" }); } catch (error) { toast(error.message, "error"); return; }
                    window.location.assign("login.html");
                });
            });
            try {
                populateSettings();
                populateAdminProfile();
                populateExpertProfile();
                await populateAdminSettings();
                await renderFarmRecords();
                await renderConsultations();
                await renderAdminUsers();
                await renderAdminCrops();
                await renderAdminFeedback();
                await renderAdminFarmers();
                await renderAdminMarket();
                await renderAdminListings();
                await renderAdminDashboard();
                await renderAdminReports();
            } catch (error) { toast(error.message, "error"); }
        }
    }

    document.addEventListener("DOMContentLoaded", start, { once: true });
})();
