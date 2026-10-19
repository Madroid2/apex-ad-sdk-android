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
  { key: "generator", name: "Creative Generator", message: "Resolving verified product evidence", state: "DRAFT", approved: false },
  { key: "review", name: "Creative Review Agent", message: "Applying policy and brand checks", state: "CHECKING", approved: false },
  { key: "copilot", name: "Campaign Copilot", message: "Explaining an action for human approval", state: "APPROVAL", approved: false },
  { key: "auction", name: "Auction Path", message: "Serving the approved creative without model latency", state: "BID READY", approved: true }
];

const agentNodes = Array.from(document.querySelectorAll("[data-agent-node]"));
const agentName = document.querySelector("[data-agent-name]");
const agentMessage = document.querySelector("[data-agent-message]");
const agentStep = document.querySelector("[data-agent-step]");
const agentRunState = document.querySelector("[data-agent-run-state]");
const agentProgress = Array.from(document.querySelectorAll("[data-agent-progress]"));
const agentToggle = document.querySelector("[data-agent-toggle]");
const agentToggleLabel = document.querySelector("[data-agent-toggle-label]");
const creativePreview = document.querySelector("[data-creative-preview]");
const creativeState = document.querySelector(".creative-state");
let agentStageIndex = 0;
let agentTimer = null;
let agentPaused = false;

function renderAgentStage() {
  const stage = agentStages[agentStageIndex];
  agentNodes.forEach((node) => node.classList.toggle("is-active", node.dataset.agentNode === stage.key));
  agentProgress.forEach((item, index) => {
    item.classList.toggle("is-active", index === agentStageIndex);
    item.classList.toggle("is-complete", index < agentStageIndex);
  });
  if (agentName) agentName.textContent = stage.name;
  if (agentMessage) agentMessage.textContent = stage.message;
  if (agentStep) agentStep.textContent = `Step ${agentStageIndex + 1} of ${agentStages.length}`;
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
  const icon = agentToggle.querySelector(".activity-toggle-icon");
  if (agentPaused) {
    window.clearInterval(agentTimer);
    if (icon) icon.textContent = "▶";
    if (agentToggleLabel) agentToggleLabel.textContent = "Resume workflow";
    if (agentRunState) agentRunState.textContent = "Workflow paused";
    agentToggle.setAttribute("aria-label", "Resume agent workflow");
    agentToggle.setAttribute("aria-pressed", "true");
    document.querySelector(".agent-activity")?.classList.add("is-paused");
  } else {
    startAgentAnimation();
    if (icon) icon.textContent = "Ⅱ";
    if (agentToggleLabel) agentToggleLabel.textContent = "Pause workflow";
    if (agentRunState) agentRunState.textContent = "Workflow running";
    agentToggle.setAttribute("aria-label", "Pause agent workflow");
    agentToggle.setAttribute("aria-pressed", "false");
    document.querySelector(".agent-activity")?.classList.remove("is-paused");
  }
});

const workbench = document.querySelector("[data-agent-workbench]");
const workbenchButtons = Array.from(document.querySelectorAll("[data-workbench-step]"));
const workbenchStages = {
  generate: {
    index: "01",
    position: "28%",
    status: "Four format variants assembled",
    eyebrow: "Creative Generator",
    title: "Turn product truth into format-ready advertising.",
    copy: "The agent resolves the landing page, validates product and brand evidence, then produces coherent Display, Native, Interstitial and VAST variants from one bounded brief.",
    input: "URL, assets, offer, audience",
    output: "Draft creative set + provenance",
    authority: "No serving permission"
  },
  review: {
    index: "02",
    position: "48%",
    status: "Policy evidence scored and recorded",
    eyebrow: "Creative Review Agent",
    title: "Separate obvious decisions from genuine judgment calls.",
    copy: "Deterministic checks run before and after model assessment. The agent records policy evidence, brand and domain consistency, confidence, and the exact policy version used for replay.",
    input: "Creative, destination, policy",
    output: "Approve, reject, or review band",
    authority: "Cannot override admin review"
  },
  gate: {
    index: "03",
    position: "68%",
    status: "Eligibility gate evaluating final state",
    eyebrow: "Deterministic Eligibility Gate",
    title: "Only an authorized decision can change serving state.",
    copy: "Hard policy failures stay rejected. High-confidence safe cases can follow configured automation, while ambiguous, sensitive, or conflicting evidence enters the admin queue for a human decision.",
    input: "Decision + evidence + actor",
    output: "Versioned eligibility state",
    authority: "Rules and human roles prevail"
  },
  auction: {
    index: "04",
    position: "89%",
    status: "Approved creative available to bidder",
    eyebrow: "Auction-Eligible Creative",
    title: "The finished ad joins bidding without bringing the model with it.",
    copy: "The bidder reads a pre-approved, cached creative and returns standard OpenRTB markup with signed trackers. Generation, reasoning and review remain entirely outside the impression-serving path.",
    input: "Eligible campaign + slot context",
    output: "OpenRTB bid response",
    authority: "Deterministic auction only"
  }
};

let workbenchIndex = 0;
let workbenchTimer = null;

function selectWorkbenchStage(key) {
  const stage = workbenchStages[key];
  if (!workbench || !stage) return;
  workbenchButtons.forEach((button, index) => {
    const active = button.dataset.workbenchStep === key;
    button.classList.toggle("is-active", active);
    button.setAttribute("aria-pressed", String(active));
    if (active) workbenchIndex = index;
  });
  workbench.querySelector(".journey-floor")?.style.setProperty("--journey-position", stage.position);
  const values = {
    "[data-workbench-status]": stage.status,
    "[data-workbench-index]": stage.index,
    "[data-workbench-eyebrow]": stage.eyebrow,
    "[data-workbench-title]": stage.title,
    "[data-workbench-copy]": stage.copy,
    "[data-workbench-input]": stage.input,
    "[data-workbench-output]": stage.output,
    "[data-workbench-authority]": stage.authority
  };
  Object.entries(values).forEach(([selector, value]) => {
    const node = workbench.querySelector(selector);
    if (node) node.textContent = value;
  });
}

function startWorkbenchAnimation() {
  window.clearInterval(workbenchTimer);
  workbenchTimer = window.setInterval(() => {
    workbenchIndex = (workbenchIndex + 1) % workbenchButtons.length;
    selectWorkbenchStage(workbenchButtons[workbenchIndex].dataset.workbenchStep);
  }, 3600);
}

workbenchButtons.forEach((button) => {
  button.addEventListener("click", () => {
    selectWorkbenchStage(button.dataset.workbenchStep);
    if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) startWorkbenchAnimation();
  });
});

if (workbenchButtons.length && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
  startWorkbenchAnimation();
}

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
