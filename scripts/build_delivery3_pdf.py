#!/usr/bin/env python3
"""Build the standalone Delivery 3 quality and backlog document."""

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.platypus import (
    Flowable,
    Frame,
    HRFlowable,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Spacer,
)
from reportlab.platypus.tableofcontents import TableOfContents

import build_project_pdf as common


ROOT = Path(__file__).resolve().parents[1]
DOCS = [
    ROOT / "docs" / "11-test-strategy.md",
    ROOT / "docs" / "12-unit-test-plan.md",
    ROOT / "docs" / "13-integration-test-plan.md",
    ROOT / "docs" / "14-qa-procedure.md",
    ROOT / "docs" / "15-quality-gate.md",
    ROOT / "docs" / "16-regression-suite.md",
    ROOT / "docs" / "17-release-checklist.md",
    ROOT / "docs" / "18-defect-management.md",
    ROOT / "docs" / "19-initial-backlog.md",
    ROOT / "docs" / "traceability" / "rule-test-traceability.md",
]
OUTPUT = ROOT / "output" / "pdf" / "POS_Minimarket_Entrega_3_Calidad_y_Backlog_v0.1.pdf"


def delivery_header_footer(canvas, doc) -> None:
    canvas.saveState()
    page_w, page_h = doc.pagesize
    canvas.setStrokeColor(common.LINE)
    canvas.setLineWidth(0.5)
    canvas.line(18 * mm, page_h - 14 * mm, page_w - 18 * mm, page_h - 14 * mm)
    canvas.setFont("DocSans", 7.2)
    canvas.setFillColor(common.MID)
    canvas.drawString(18 * mm, page_h - 10 * mm, "Sistema POS e inventario - Entrega 3")
    canvas.drawRightString(page_w - 18 * mm, 10 * mm, f"Pagina {doc.page}")
    canvas.restoreState()


def build_pdf() -> Path:
    common.register_fonts()
    styles = common.build_styles()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    portrait_size = A4
    landscape_size = landscape(A4)
    portrait_frame = Frame(
        18 * mm,
        16 * mm,
        portrait_size[0] - 36 * mm,
        portrait_size[1] - 33 * mm,
        id="portrait-frame",
    )
    landscape_frame = Frame(
        18 * mm,
        16 * mm,
        landscape_size[0] - 36 * mm,
        landscape_size[1] - 33 * mm,
        id="landscape-frame",
    )
    cover_frame = Frame(
        28 * mm,
        28 * mm,
        portrait_size[0] - 56 * mm,
        portrait_size[1] - 56 * mm,
        id="cover-frame",
        showBoundary=0,
    )

    pdf = common.ProjectDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        title="Sistema POS e inventario - Entrega 3",
        author="Proyecto técnico",
        subject="Estrategia de calidad, QA y backlog - propuesta para aprobación",
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=18 * mm,
        bottomMargin=16 * mm,
    )
    pdf.addPageTemplates(
        [
            PageTemplate(id="cover", pagesize=A4, frames=[cover_frame], onPage=common.cover_page),
            PageTemplate(id="portrait", pagesize=A4, frames=[portrait_frame], onPage=delivery_header_footer),
            PageTemplate(id="landscape", pagesize=landscape_size, frames=[landscape_frame], onPage=delivery_header_footer),
        ]
    )

    story: list[Flowable] = [
        Spacer(1, 54 * mm),
        Paragraph(
            "ENTREGA 3",
            ParagraphStyle(
                "CoverKicker",
                fontName="DocSans-Bold",
                fontSize=12,
                leading=16,
                textColor=colors.HexColor("#93C5FD"),
                alignment=TA_LEFT,
                spaceAfter=8,
            ),
        ),
        Paragraph(
            "Calidad, QA y backlog",
            ParagraphStyle(
                "CoverTitle",
                fontName="DocSans-Bold",
                fontSize=29,
                leading=35,
                textColor=common.WHITE,
                alignment=TA_LEFT,
                spaceAfter=14,
            ),
        ),
        Paragraph(
            "Sistema POS e inventario para minimarket familiar",
            ParagraphStyle(
                "CoverSub",
                fontName="DocSans",
                fontSize=14,
                leading=20,
                textColor=colors.HexColor("#DCE8FF"),
                alignment=TA_LEFT,
                spaceAfter=22,
            ),
        ),
        HRFlowable(width="58%", thickness=2, color=common.BLUE, spaceBefore=4, spaceAfter=18, hAlign="LEFT"),
        Paragraph(
            "Estrategia y planes de prueba, procedimiento de QA, gates, regresión, liberación, defectos, backlog y trazabilidad completa.",
            ParagraphStyle(
                "CoverDesc",
                fontName="DocSans",
                fontSize=10.3,
                leading=16,
                textColor=common.WHITE,
                alignment=TA_LEFT,
                spaceAfter=18,
            ),
        ),
        Paragraph(
            "Documento independiente. Propuesta para aprobación; no autoriza todavía el inicio de código.",
            ParagraphStyle(
                "CoverRule",
                fontName="DocSans-Bold",
                fontSize=9.1,
                leading=14,
                textColor=colors.HexColor("#BFDBFE"),
                alignment=TA_LEFT,
                borderColor=colors.HexColor("#3B82F6"),
                borderWidth=0.8,
                borderPadding=8,
                backColor=colors.HexColor("#1E3A5F"),
            ),
        ),
        Spacer(1, 38 * mm),
        Paragraph(
            "Versión 0.1 | 31 de julio de 2026",
            ParagraphStyle("CoverFoot", fontName="DocSans", fontSize=8, textColor=colors.HexColor("#CBD5E1")),
        ),
        NextPageTemplate("portrait"),
        PageBreak(),
        Paragraph("Contenido", styles["h1"]),
    ]

    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle(
            "TOC1",
            fontName="DocSans-Bold",
            fontSize=9.5,
            leading=14,
            textColor=common.NAVY,
            leftIndent=0,
            firstLineIndent=0,
            spaceBefore=4,
        ),
        ParagraphStyle(
            "TOC2",
            fontName="DocSans",
            fontSize=8.2,
            leading=12,
            textColor=common.TEXT,
            leftIndent=12,
            firstLineIndent=0,
        ),
        ParagraphStyle(
            "TOC3",
            fontName="DocSans",
            fontSize=7.4,
            leading=10,
            textColor=common.MID,
            leftIndent=24,
            firstLineIndent=0,
        ),
    ]
    story.extend([toc, PageBreak()])

    for path in DOCS:
        story.extend(common.parse_markdown(path, styles))
        if path != DOCS[-1]:
            story.append(PageBreak())

    story.extend(
        [
            PageBreak(),
            common.heading("Cierre de la Entrega 3", 1, styles),
            Paragraph(
                "Esta propuesta define cómo se demostrará la calidad y en qué orden se construirá el MVP. La aprobación de la Entrega 3 permitirá refinar la Iteración 0. El inicio de implementación seguirá esperando una instrucción explícita del usuario.",
                styles["lead"],
            ),
            Paragraph(
                "Restricción vigente: recursos locales o planes gratuitos; ningún servicio de pago se incorporará sin aprobación explícita.",
                styles["body"],
            ),
        ]
    )

    pdf.multiBuild(story)
    return OUTPUT


if __name__ == "__main__":
    print(build_pdf())
