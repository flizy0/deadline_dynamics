"use strict";
if (window.lucide) window.lucide.createIcons();

const sidebar = document.querySelector("[data-sidebar]");
const navToggle = document.querySelector("[data-nav-toggle]");
const navClose = sidebar && sidebar.querySelector("[data-nav-close]");
const navOverlay = document.querySelector("[data-nav-overlay]");
const workspace = document.querySelector("[data-workspace]");

if (sidebar && navToggle && navClose && navOverlay && workspace) {
    const mobileNavigation = window.matchMedia("(max-width: 1023px)");
    const originalRole = sidebar.getAttribute("role");
    const originalLabel = sidebar.getAttribute("aria-label");
    const dialogLabel = originalLabel || sidebar.querySelector("nav[aria-label]")?.getAttribute("aria-label");
    let navigationOpen = false;
    const opener = navToggle;

    const focusableItems = () => Array.from(sidebar.querySelectorAll(
        "a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), " +
        "textarea:not([disabled]), [tabindex]:not([tabindex='-1'])"
    )).filter(item => !item.closest("[hidden], [inert]") && item.getClientRects().length > 0);

    const focusFirstItem = () => {
        const firstItem = focusableItems()[0];
        if (firstItem) {
            firstItem.focus();
        } else {
            sidebar.tabIndex = -1;
            sidebar.focus();
        }
    };

    const restoreSidebarSemantics = () => {
        if (originalRole) sidebar.setAttribute("role", originalRole);
        else sidebar.removeAttribute("role");
        if (originalLabel) sidebar.setAttribute("aria-label", originalLabel);
        else sidebar.removeAttribute("aria-label");
        sidebar.removeAttribute("aria-modal");
    };

    const closeNavigation = (restoreFocus = true) => {
        const wasOpen = navigationOpen;
        navigationOpen = false;
        document.body.classList.remove("nav-open");
        navToggle.setAttribute("aria-expanded", "false");
        navOverlay.hidden = true;
        workspace.inert = false;
        restoreSidebarSemantics();
        sidebar.inert = mobileNavigation.matches;
        if (mobileNavigation.matches) sidebar.setAttribute("aria-hidden", "true");
        else sidebar.removeAttribute("aria-hidden");
        if (restoreFocus && wasOpen && opener.isConnected) opener.focus();
    };

    const openNavigation = () => {
        if (!mobileNavigation.matches || navigationOpen) return;
        navigationOpen = true;
        sidebar.inert = false;
        sidebar.removeAttribute("aria-hidden");
        sidebar.setAttribute("role", "dialog");
        sidebar.setAttribute("aria-modal", "true");
        if (dialogLabel) sidebar.setAttribute("aria-label", dialogLabel);
        navToggle.setAttribute("aria-expanded", "true");
        navOverlay.hidden = false;
        document.body.classList.add("nav-open");
        workspace.inert = true;
        focusFirstItem();
    };

    navToggle.addEventListener("click", () => {
        if (navigationOpen) closeNavigation();
        else openNavigation();
    });
    navClose.addEventListener("click", () => closeNavigation());
    navOverlay.addEventListener("click", () => closeNavigation());
    sidebar.querySelectorAll("a[href]").forEach(link => {
        link.addEventListener("click", () => closeNavigation(false));
    });

    document.addEventListener("keydown", event => {
        if (!navigationOpen) return;
        if (event.key === "Escape") {
            event.preventDefault();
            closeNavigation();
            return;
        }
        if (event.key !== "Tab") return;
        const items = focusableItems();
        const firstItem = items[0];
        const lastItem = items[items.length - 1];
        if (!firstItem) {
            event.preventDefault();
            focusFirstItem();
        } else if (event.shiftKey && (document.activeElement === firstItem || !sidebar.contains(document.activeElement))) {
            event.preventDefault();
            lastItem.focus();
        } else if (!event.shiftKey && (document.activeElement === lastItem || !sidebar.contains(document.activeElement))) {
            event.preventDefault();
            firstItem.focus();
        }
    });

    document.addEventListener("focusin", event => {
        if (navigationOpen && !sidebar.contains(event.target)) focusFirstItem();
    });

    mobileNavigation.addEventListener("change", () => {
        const focusWasInside = sidebar.contains(document.activeElement);
        const wasOpen = navigationOpen;
        closeNavigation(false);
        if (mobileNavigation.matches && (focusWasInside || wasOpen)) navToggle.focus();
        else if (!mobileNavigation.matches && wasOpen) {
            const currentLink = sidebar.querySelector("a[aria-current='page'], a.active, a[href]");
            if (currentLink) currentLink.focus();
        }
    });

    document.body.classList.add("js-ready");
    closeNavigation(false);
}

const surveyForm = document.querySelector(".survey-form");
if (surveyForm) {
    const questions = surveyForm.querySelectorAll("fieldset:not([data-field='eligible'])");
    const updateEligibility = () => {
        const eligible = surveyForm.querySelector("input[name='eligible']:checked");
        const no = eligible && eligible.value === "NO";
        questions.forEach(question => {
            question.hidden = no;
            question.disabled = no;
            question.querySelectorAll("input").forEach(input => { input.required = !no; });
        });
    };
    surveyForm.querySelectorAll("input[name='eligible']").forEach(input => {
        input.addEventListener("change", updateEligibility);
    });
    updateEligibility();
}
