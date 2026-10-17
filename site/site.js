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
    menuButton.setAttribute("aria-label", open ? "Close navigation" : "Open navigation");
  });

  menu.querySelectorAll("a").forEach((link) => {
    link.addEventListener("click", () => {
      menu.classList.remove("open");
      menuButton.setAttribute("aria-expanded", "false");
      menuButton.setAttribute("aria-label", "Open navigation");
    });
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
  if (event.key === "Escape" && menu?.classList.contains("open")) {
    menu.classList.remove("open");
    menuButton?.setAttribute("aria-expanded", "false");
    menuButton?.setAttribute("aria-label", "Open navigation");
  }
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
    event.preventDefault();
    document.querySelector("[data-open-search]")?.click();
  }
});

document.querySelectorAll("[data-year]").forEach((element) => {
  element.textContent = new Date().getFullYear();
});

const platformTabs = Array.from(document.querySelectorAll("[data-platform-tab]"));
const platformPanels = Array.from(document.querySelectorAll("[data-platform-panel]"));

function selectPlatformStage(stage) {
  platformTabs.forEach((tab) => {
    const active = tab.dataset.platformTab === stage;
    tab.classList.toggle("is-active", active);
    tab.setAttribute("aria-selected", String(active));
    tab.tabIndex = active ? 0 : -1;
  });
  platformPanels.forEach((panel) => {
    panel.hidden = panel.dataset.platformPanel !== stage;
  });
}

platformTabs.forEach((tab, index) => {
  tab.addEventListener("click", () => selectPlatformStage(tab.dataset.platformTab));
  tab.addEventListener("keydown", (event) => {
    if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return;
    event.preventDefault();
    const direction = event.key === 'ArrowRight' ? 1 : -1;
    const next = platformTabs[(index + direction + platformTabs.length) % platformTabs.length];
    selectPlatformStage(next.dataset.platformTab);
    next.focus();
  });
});

const agentStages = [
  { key: "generator", message: "Resolving landing-page evidence", state: "DRAFT", approved: false },
  { key: "review", message: "Checking policy and brand evidence", state: "CHECKING", approved: false },
  { key: "copilot", message: "Preparing an informed action proposal", state: "APPROVAL", approved: false },
  { key: "auction", message: "Approved creative is bid-ready", state: "BID READY", approved: true }
];

const agentNodes = Array.from(document.querySelectorAll("[data-agent-node]"));
const agentMessage = document.querySelector("[data-agent-message]");
const agentToggle = document.querySelector("[data-agent-toggle]");
const creativePreview = document.querySelector("[data-creative-preview]");
const creativeState = document.querySelector(".creative-state");
let agentStageIndex = 0;
let agentTimer = null;
let agentPaused = false;

function renderAgentStage() {
  const stage = agentStages[agentStageIndex];
  agentNodes.forEach((node) => node.classList.toggle("is-active", node.dataset.agentNode === stage.key));
  if (agentMessage) agentMessage.textContent = stage.message;
  if (creativeState) creativeState.textContent = stage.state;
  creativePreview?.classList.toggle("is-approved", stage.approved);
}

function startAgentAnimation() {
  window.clearInterval(agentTimer);
  agentTimer = window.setInterval(() => {
    agentStageIndex = (agentStageIndex + 1) % agentStages.length;
    renderAgentStage();
  }, 2200);
}

if (agentNodes.length) {
  renderAgentStage();
  if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) startAgentAnimation();
}

agentToggle?.addEventListener("click", () => {
  agentPaused = !agentPaused;
  if (agentPaused) {
    window.clearInterval(agentTimer);
    agentToggle.querySelector("span").textContent = ">";
    agentToggle.setAttribute("aria-label", "Play agent animation");
    agentToggle.title = "Play agent animation";
  } else {
    startAgentAnimation();
    agentToggle.querySelector("span").textContent = "II";
    agentToggle.setAttribute("aria-label", "Pause agent animation");
    agentToggle.title = "Pause agent animation";
  }
});

const revealItems = document.querySelectorAll(".reveal");
if ("IntersectionObserver" in window && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
  const revealObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("is-visible");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.14 });
  revealItems.forEach((item) => revealObserver.observe(item));
} else {
  revealItems.forEach((item) => item.classList.add("is-visible"));
}
