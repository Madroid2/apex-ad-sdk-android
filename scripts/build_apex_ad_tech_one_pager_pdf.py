#!/usr/bin/env python3
"""Build the Apex Ad Tech CTO one-page brief."""

from __future__ import annotations

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfbase.pdfmetrics import stringWidth
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "pdf" / "Apex_Ad_Tech_One_Pager.pdf"

PAGE_W, PAGE_H = A4
MARGIN = 14 * mm
CONTENT_W = PAGE_W - (2 * MARGIN)

NAVY = colors.HexColor("#071225")
INK = colors.HexColor("#142238")
MUTED = colors.HexColor("#63718A")
BLUE = colors.HexColor("#5273EA")
TEAL = colors.HexColor("#148A80")
PALE_BLUE = colors.HexColor("#EDF3FF")
PALE_TEAL = colors.HexColor("#EAF8F5")
PALE_GRAY = colors.HexColor("#F4F6F9")
BORDER = colors.HexColor("#D7DFEA")
WHITE = colors.white


def wrap_lines(text: str, font: str, size: float, width: float) -> list[str]:
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if stringWidth(candidate, font, size) <= width:
            current = candidate
        else:
            if current:
                lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


def draw_wrapped(
    c: canvas.Canvas,
    text: str,
    x: float,
    y: float,
    width: float,
    font: str = "Helvetica",
    size: float = 7.8,
    leading: float = 10.2,
    color=INK,
) -> float:
    c.setFont(font, size)
    c.setFillColor(color)
    for line in wrap_lines(text, font, size, width):
        c.drawString(x, y, line)
        y -= leading
    return y


def draw_bullet(c: canvas.Canvas, text: str, x: float, y: float, width: float) -> float:
    c.setFillColor(BLUE)
    c.circle(x + 2.2, y + 2.15, 1.65, fill=1, stroke=0)
    return draw_wrapped(c, text, x + 10, y, width - 10, size=7.5, leading=9.25) - 3.2


def draw_section_title(c: canvas.Canvas, text: str, x: float, y: float) -> float:
    c.setFillColor(BLUE)
    c.setFont("Helvetica-Bold", 10.4)
    c.drawString(x, y, text.upper())
    c.setStrokeColor(BORDER)
    c.setLineWidth(0.55)
    c.line(x, y - 4, x + 18 * mm, y - 4)
    return y - 16


def draw_architecture(c: canvas.Canvas, top: float) -> None:
    c.setFillColor(PALE_GRAY)
    c.roundRect(MARGIN, top - 31 * mm, CONTENT_W, 31 * mm, 5, fill=1, stroke=0)
    c.setFont("Helvetica-Bold", 7.2)
    c.setFillColor(MUTED)
    c.drawString(MARGIN + 7, top - 11, "END-TO-END SYSTEM")

    gap = 8
    box_w = (CONTENT_W - 30 - (3 * gap)) / 4
    x = MARGIN + 15
    y = top - 27 * mm
    labels = [
        ("ANDROID / RN SDK", "render, privacy, trust"),
        ("GO AD SERVER", "auction, quality, receipts"),
        ("APEX PLATFORM", "demand, supply, copilots"),
        ("INTELLIGENCE", "gated pCTR lifecycle"),
    ]
    for index, (title, subtitle) in enumerate(labels):
        fill = PALE_BLUE if index % 2 == 0 else PALE_TEAL
        c.setFillColor(fill)
        c.setStrokeColor(BORDER)
        c.roundRect(x, y, box_w, 13 * mm, 4, fill=1, stroke=1)
        c.setFillColor(INK)
        c.setFont("Helvetica-Bold", 7.1)
        c.drawCentredString(x + box_w / 2, y + 8.1 * mm, title)
        c.setFillColor(MUTED)
        c.setFont("Helvetica", 6.6)
        c.drawCentredString(x + box_w / 2, y + 4.1 * mm, subtitle)
        if index < len(labels) - 1:
            arrow_x = x + box_w + 2
            c.setStrokeColor(BLUE)
            c.setLineWidth(1)
            c.line(arrow_x, y + 6.5 * mm, arrow_x + gap - 3, y + 6.5 * mm)
            c.line(arrow_x + gap - 6, y + 6.5 * mm + 2, arrow_x + gap - 3, y + 6.5 * mm)
            c.line(arrow_x + gap - 6, y + 6.5 * mm - 2, arrow_x + gap - 3, y + 6.5 * mm)
        x += box_w + gap


def build_pdf() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(
        str(OUTPUT),
        pagesize=A4,
        pageCompression=1,
    )
    c.setTitle("Apex Ad Tech - Android SDK-led programmatic stack")
    c.setAuthor("Madhur Lalit")
    c.setSubject("One-page technical brief for CTO discussion")

    header_h = 47 * mm
    c.setFillColor(NAVY)
    c.rect(0, PAGE_H - header_h, PAGE_W, header_h, fill=1, stroke=0)
    c.setFillColor(BLUE)
    c.rect(0, PAGE_H - 4 * mm, PAGE_W * 0.7, 4 * mm, fill=1, stroke=0)
    c.setFillColor(TEAL)
    c.rect(PAGE_W * 0.7, PAGE_H - 4 * mm, PAGE_W * 0.3, 4 * mm, fill=1, stroke=0)

    c.setFillColor(colors.HexColor("#AFC0FF"))
    c.setFont("Helvetica-Bold", 8.4)
    c.drawString(MARGIN, PAGE_H - 15 * mm, "APEX AD TECH  |  TECHNICAL BRIEF")
    c.setFillColor(WHITE)
    c.setFont("Helvetica-Bold", 23)
    c.drawString(MARGIN, PAGE_H - 28 * mm, "Android SDK-led programmatic stack")
    draw_wrapped(
        c,
        "A modular Android monetization SDK, Go SSP/auction engine and agentic control plane built as one end-to-end system.",
        MARGIN,
        PAGE_H - 36 * mm,
        CONTENT_W * 0.78,
        size=9.4,
        leading=12,
        color=colors.HexColor("#CED8F3"),
    )
    c.setFillColor(colors.HexColor("#A8B7D8"))
    c.setFont("Helvetica", 7.4)
    c.drawRightString(PAGE_W - MARGIN, PAGE_H - 43 * mm, "Built independently by Madhur Lalit  |  August 2026")

    architecture_top = PAGE_H - header_h - 6 * mm
    draw_architecture(c, architecture_top)

    main_top = architecture_top - 37 * mm
    gutter = 9 * mm
    column_w = (CONTENT_W - gutter) / 2
    left_x = MARGIN
    right_x = MARGIN + column_w + gutter

    y_left = draw_section_title(c, "Android SDK - implemented", left_x, main_top)
    bullets_left = [
        "Banner/MRECT, Interstitial, IAB Native 1.2, VAST 4.0 rewarded video, App Open, In-App Bidding and MRAID 3.0 over OpenRTB 2.6.",
        "API 21+ modular AARs; zero third-party runtime dependencies in sdk-core; lifecycle-owned ad objects, consumer rules and 40 unit/instrumentation test source files.",
        "Consent-aware GAID/LAT, TCF keys, GPP, US Privacy, COPPA/LMT, consent-gated first-party cohorts, real WebView UA and schain propagation.",
        "Optional Play Integrity leases plus Android Keystore P-256 signing; replay-resistant tracking with separate win, bill and loss lifecycle and a persistent retry queue.",
        "Native Android demo, AdMob adapter, bidding-token flow and a working React Native bridge for Android.",
    ]
    for item in bullets_left:
        y_left = draw_bullet(c, item, left_x, y_left, column_w)

    y_left -= 1
    y_left = draw_section_title(c, "Differentiated format work", left_x, y_left)
    differentiated = [
        "Conversational Ads for AI assistants: a newly introduced, disclosed programmatic Native format for AI-assistant chats, which most Ad Tech SDKs do not yet expose. Registered taxonomy only; no raw chat leaves the app.",
        "Wallet Pass Ads: Save to Google Wallet as an action layer on standard MRECT, interstitial or Native demand, with ordinary display fallback.",
        "Intent-to-Action Native: disclosed executable actions negotiated against capabilities actually installed in the app.",
    ]
    for item in differentiated:
        y_left = draw_bullet(c, item, left_x, y_left, column_w)

    y_right = draw_section_title(c, "Server and control plane", right_x, main_top)
    bullets_right = [
        "Go SSP: first-price parallel demand fan-out, publisher/app-ads.txt registry, per-placement floors, quality gates, dynamic floor experiments, signed lifecycle callbacks and auditable auction receipts.",
        "Apex Intelligence: privacy-minimized contracts, real-data gates, FTRL pCTR training, model registry, human promotion and shadow-first scoring that fails closed without evidence.",
    ]
    for item in bullets_right:
        y_right = draw_bullet(c, item, right_x, y_right, column_w)

    y_right -= 1
    y_right = draw_section_title(c, "AI agents for Ad Tech bottlenecks", right_x, y_right)
    agent_bullets = [
        "Creative Generation Agent: converts landing pages or product briefs into multi-format drafts, reducing creative-production and setup delays.",
        "Creative Review Agent: versioned policy, brand/domain, landing-page and asset checks with audit evidence and manual escalation reduce approval delays.",
        "Campaign Copilot: evidence-grounded delivery, budget, bidding, creative and tracking diagnosis reduces buyer and AdOps troubleshooting time.",
        "Publisher Copilot: placement and revenue guidance plus SDK, Gradle/runtime, crash and ANR triage reduces integration and support delays, with engineering escalation.",
    ]
    for item in agent_bullets:
        y_right = draw_bullet(c, item, right_x, y_right, column_w)

    y_right = draw_wrapped(
        c,
        "CONTROL-PLANE SAFETY: agents are approval-gated and auditable; auctions and rendering remain deterministic.",
        right_x + 10,
        y_right + 1,
        column_w - 10,
        font="Helvetica-Bold",
        size=6.7,
        leading=8.2,
        color=TEAL,
    ) - 2

    y_right = draw_section_title(c, "Readiness boundary", right_x, y_right)
    c.setFillColor(PALE_TEAL)
    c.setStrokeColor(BORDER)
    readiness_h = 32 * mm
    c.roundRect(right_x, y_right - readiness_h + 6, column_w, readiness_h, 5, fill=1, stroke=1)
    c.setFillColor(TEAL)
    c.setFont("Helvetica-Bold", 7.6)
    c.drawString(right_x + 8, y_right - 6, "WORKING NOW")
    working_y = draw_wrapped(
        c,
        "SDK-to-auction-to-demand flow; standard rendering; privacy/trust contracts; publisher controls; creative generation/review; audit foundations.",
        right_x + 8,
        y_right - 17,
        column_w - 16,
        size=6.8,
        leading=8.2,
    )
    c.setFillColor(BLUE)
    c.setFont("Helvetica-Bold", 7.6)
    c.drawString(right_x + 8, working_y - 2, "NEXT PRODUCTION GATES")
    draw_wrapped(
        c,
        "IAB OM SDK certification; fleet telemetry and kill switch; Maven/API guarantees; device, leak, ANR, fuzz, load, privacy and security validation. OMID remains honesty-gated until certified.",
        right_x + 8,
        working_y - 13,
        column_w - 16,
        size=6.8,
        leading=8.2,
    )

    relevance_y = 42 * mm
    relevance_h = 43 * mm
    c.setFillColor(PALE_BLUE)
    c.setStrokeColor(BORDER)
    c.roundRect(MARGIN, relevance_y, CONTENT_W, relevance_h, 6, fill=1, stroke=1)
    c.setFillColor(BLUE)
    c.setFont("Helvetica-Bold", 9.8)
    c.drawString(
        MARGIN + 10,
        relevance_y + relevance_h - 15,
        "KEY DIFFERENTIATORS FROM COMMONLY USED AD TECH PRODUCTS",
    )

    cards = [
        ("1", "SDK-direct supply", "A first-party mobile supply path with clearer ownership, signals and auction transparency."),
        ("2", "Conversational Ads", "A dedicated, disclosed programmatic Native format for AI-assistant chats, absent from most Ad Tech SDKs."),
        ("3", "AI workflow agents", "Generation, review and buyer/publisher Copilots remove production, approval, operations and support bottlenecks."),
    ]
    card_w = (CONTENT_W - 34) / 3
    for index, (number, title, body) in enumerate(cards):
        x = MARGIN + 10 + index * (card_w + 7)
        item_y = relevance_y + relevance_h - 34
        text_x = x + 19
        c.setFillColor(BLUE if index != 1 else TEAL)
        c.circle(x + 7, item_y, 7, fill=1, stroke=0)
        c.setFillColor(WHITE)
        c.setFont("Helvetica-Bold", 8.2)
        c.drawCentredString(x + 7, item_y - 2.8, number)
        c.setFillColor(INK)
        c.setFont("Helvetica-Bold", 7.8)
        c.drawString(text_x, item_y - 2.8, title)
        draw_wrapped(c, body, text_x, item_y - 16, card_w - 19, size=6.9, leading=8.6, color=MUTED)

    footer_y = 23 * mm
    c.setStrokeColor(BORDER)
    c.line(MARGIN, footer_y + 18, PAGE_W - MARGIN, footer_y + 18)
    c.setFillColor(MUTED)
    c.setFont("Helvetica-Bold", 8.0)
    c.drawString(MARGIN, footer_y + 6, "GitHub reference for technical deep dives")
    c.setFillColor(BLUE)
    c.setFont("Helvetica", 7.7)
    url = "https://github.com/Madroid2/apex-ad-sdk-android"
    c.drawRightString(PAGE_W - MARGIN, footer_y + 6, url)
    c.linkURL(url, (PAGE_W - MARGIN - stringWidth(url, "Helvetica", 7.7), footer_y, PAGE_W - MARGIN, footer_y + 14), relative=0)

    c.showPage()
    c.save()
    print(OUTPUT)


if __name__ == "__main__":
    build_pdf()
