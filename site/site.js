const docsIndex = [
  { title: "Install the SDK", detail: "Maven coordinates and module selection", href: "/docs/#install" },
  { title: "Initialize Apex", detail: "Application setup and configuration", href: "/docs/#initialize" },
  { title: "Banner ads", detail: "320x50 and MRECT rendering", href: "/docs/formats.html#banner" },
  { title: "Interstitial ads", detail: "Preload and show at natural breaks", href: "/docs/formats.html#interstitial" },
  { title: "Native ads", detail: "IAB Native 1.2 publisher-rendered UI", href: "/docs/formats.html#native" },
  { title: "Conversational sponsored suggestions", detail: "Sponsored cards for AI assistant surfaces", href: "/docs/formats.html#conversational" },
  { title: "VAST video", detail: "Rewarded video and quartile tracking", href: "/docs/formats.html#video" },
  { title: "App Open", detail: "Lifecycle, expiry and frequency caps", href: "/docs/formats.html#app-open" },
  { title: "Wallet actions", detail: "Optional Save to Google Wallet outcomes", href: "/docs/formats.html#wallet" },
  { title: "Privacy and consent", detail: "TCF, GPP and data minimization", href: "/docs/trust.html#privacy" },
  { title: "Trust layer", detail: "Play Integrity, request signing and replay protection", href: "/docs/trust.html#trust" },
  { title: "Production activation", detail: "External credentials and live acceptance gates", href: "/docs/trust.html#production" },
  { title: "Maven Central release", detail: "net.apexsdk publication status", href: "/docs/#release-status" }
];

const rootPrefix = document.documentElement.dataset.root || "";

document.querySelectorAll("[data-copy]").forEach((button) => {
  button.addEventListener("click", async () => {
    const target = document.getElementById(button.dataset.copy);
    if (!target) return;
    const text = target.textContent.trim();
    try {
      await navigator.clipboard.writeText(text);
      const original = button.textContent;
      button.textContent = "Copied";
      window.setTimeout(() => { button.textContent = original; }, 1500);
    } catch {
      button.textContent = "Select";
      const selection = window.getSelection();
      const range = document.createRange();
      range.selectNodeContents(target);
      selection.removeAllRanges();
      selection.addRange(range);
    }
  });
});

const menuButton = document.querySelector("[data-menu]");
const menu = document.querySelector("[data-nav]");
if (menuButton && menu) {
  menuButton.addEventListener("click", () => {
    const open = menu.classList.toggle("open");
    menuButton.setAttribute("aria-expanded", String(open));
  });
}

const dialog = document.querySelector("[data-search-dialog]");
const searchInput = document.querySelector("[data-search-input]");
const results = document.querySelector("[data-search-results]");

function renderResults(query = "") {
  if (!results) return;
  const normalized = query.trim().toLowerCase();
  const matches = docsIndex.filter((item) =>
    `${item.title} ${item.detail}`.toLowerCase().includes(normalized)
  );
  results.replaceChildren();
  if (!matches.length) {
    const empty = document.createElement("div");
    empty.className = "search-empty";
    empty.textContent = "No documentation matches that search.";
    results.appendChild(empty);
    return;
  }
  matches.forEach((item) => {
    const link = document.createElement("a");
    link.className = "search-result";
    link.href = `${rootPrefix}${item.href}`;
    const title = document.createElement("strong");
    title.textContent = item.title;
    const detail = document.createElement("span");
    detail.textContent = item.detail;
    link.append(title, detail);
    results.appendChild(link);
  });
}

document.querySelectorAll("[data-open-search]").forEach((button) => {
  button.addEventListener("click", () => {
    if (!dialog) return;
    renderResults("");
    dialog.showModal();
    window.setTimeout(() => searchInput?.focus(), 20);
  });
});

document.querySelector("[data-close-search]")?.addEventListener("click", () => dialog?.close());
searchInput?.addEventListener("input", (event) => renderResults(event.target.value));

document.addEventListener("keydown", (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    document.querySelector("[data-open-search]")?.click();
  }
});

document.querySelectorAll("[data-year]").forEach((element) => {
  element.textContent = new Date().getFullYear();
});
