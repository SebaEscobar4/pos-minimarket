#!/usr/bin/env python3
"""Build the approved POS project documentation as a polished PDF."""

from __future__ import annotations

import html
import math
import re
from pathlib import Path

from reportlab.graphics.shapes import Drawing, Line, Polygon, Rect, String
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    BaseDocTemplate,
    Flowable,
    Frame,
    HRFlowable,
    KeepTogether,
    LongTable,
    NextPageTemplate,
    PageBreak,
    PageTemplate,
    Paragraph,
    Preformatted,
    Spacer,
    Table,
    TableStyle,
)
from reportlab.platypus.tableofcontents import TableOfContents


ROOT = Path(__file__).resolve().parents[1]
DOCS = [ROOT / "docs" / f"{number:02d}-{name}.md" for number, name in [
    (1, "product-vision"),
    (2, "scope-mvp"),
    (3, "domain-glossary"),
    (4, "actors-and-use-cases"),
    (5, "business-rules"),
    (6, "domain-model"),
    (7, "module-map"),
    (8, "architecture"),
    (9, "threat-model"),
    (10, "security-baseline"),
]]
ADRS = sorted((ROOT / "adr").glob("ADR-*.md"))
OUTPUT = ROOT / "output" / "pdf" / "POS_Minimarket_Documentacion_Aprobada_v0.2.pdf"


NAVY = colors.HexColor("#14213D")
BLUE = colors.HexColor("#2563EB")
LIGHT_BLUE = colors.HexColor("#EAF1FF")
PALE = colors.HexColor("#F5F7FA")
MID = colors.HexColor("#64748B")
TEXT = colors.HexColor("#1F2937")
GREEN = colors.HexColor("#0F766E")
LINE = colors.HexColor("#CBD5E1")
WHITE = colors.white


def register_fonts() -> None:
    paths = {
        "DocSans": "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "DocSans-Bold": "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "DocSans-Oblique": "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "DocMono": "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",
    }
    for name, path in paths.items():
        pdfmetrics.registerFont(TTFont(name, path))
    pdfmetrics.registerFontFamily(
        "DocSans", normal="DocSans", bold="DocSans-Bold", italic="DocSans-Oblique"
    )


def clean_text(value: str) -> str:
    return (
        value.replace("\u2010", "-")
        .replace("\u2011", "-")
        .replace("\u2012", "-")
        .replace("\u2013", "-")
        .replace("\u2014", "-")
        .replace("\u2212", "-")
        .replace("\u00a0", " ")
    )


def inline_markup(value: str) -> str:
    value = clean_text(value)
    placeholders: list[str] = []

    def stash_code(match: re.Match[str]) -> str:
        token = f"@@CODE{len(placeholders)}@@"
        placeholders.append(
            f'<font name="DocMono" color="#0F3D77">{html.escape(match.group(1))}</font>'
        )
        return token

    value = re.sub(r"`([^`]+)`", stash_code, value)
    value = html.escape(value)
    value = re.sub(r"\[([^\]]+)\]\((https?://[^)]+)\)", r'<a href="\2" color="#2563EB">\1</a>', value)
    value = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", value)
    value = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<i>\1</i>", value)
    for index, replacement in enumerate(placeholders):
        value = value.replace(f"@@CODE{index}@@", replacement)
    return value


def build_styles():
    base = getSampleStyleSheet()
    styles = {
        "body": ParagraphStyle(
            "Body",
            parent=base["BodyText"],
            fontName="DocSans",
            fontSize=9.1,
            leading=13.1,
            textColor=TEXT,
            spaceAfter=5,
            alignment=TA_LEFT,
        ),
        "lead": ParagraphStyle(
            "Lead",
            parent=base["BodyText"],
            fontName="DocSans",
            fontSize=11,
            leading=16,
            textColor=colors.HexColor("#334155"),
            spaceAfter=10,
        ),
        "h1": ParagraphStyle(
            "Heading1",
            parent=base["Heading1"],
            fontName="DocSans-Bold",
            fontSize=20,
            leading=25,
            textColor=NAVY,
            spaceBefore=4,
            spaceAfter=13,
            keepWithNext=True,
        ),
        "h2": ParagraphStyle(
            "Heading2",
            parent=base["Heading2"],
            fontName="DocSans-Bold",
            fontSize=13.5,
            leading=17,
            textColor=BLUE,
            spaceBefore=12,
            spaceAfter=7,
            keepWithNext=True,
        ),
        "h3": ParagraphStyle(
            "Heading3",
            parent=base["Heading3"],
            fontName="DocSans-Bold",
            fontSize=10.5,
            leading=14,
            textColor=GREEN,
            spaceBefore=8,
            spaceAfter=5,
            keepWithNext=True,
        ),
        "bullet": ParagraphStyle(
            "Bullet",
            parent=base["BodyText"],
            fontName="DocSans",
            fontSize=8.9,
            leading=12.5,
            leftIndent=15,
            firstLineIndent=-8,
            bulletIndent=5,
            textColor=TEXT,
            spaceAfter=3,
        ),
        "number": ParagraphStyle(
            "Number",
            parent=base["BodyText"],
            fontName="DocSans",
            fontSize=8.9,
            leading=12.5,
            leftIndent=18,
            firstLineIndent=-11,
            bulletIndent=2,
            textColor=TEXT,
            spaceAfter=3,
        ),
        "meta": ParagraphStyle(
            "Meta",
            parent=base["BodyText"],
            fontName="DocSans",
            fontSize=8,
            leading=11,
            textColor=MID,
            spaceAfter=2,
        ),
        "cell": ParagraphStyle(
            "Cell",
            parent=base["BodyText"],
            fontName="DocSans",
            fontSize=7.1,
            leading=9.2,
            textColor=TEXT,
        ),
        "cell_small": ParagraphStyle(
            "CellSmall",
            parent=base["BodyText"],
            fontName="DocSans",
            fontSize=6.2,
            leading=8,
            textColor=TEXT,
        ),
        "cell_head": ParagraphStyle(
            "CellHead",
            parent=base["BodyText"],
            fontName="DocSans-Bold",
            fontSize=7.2,
            leading=9.2,
            textColor=WHITE,
        ),
        "code": ParagraphStyle(
            "Code",
            parent=base["Code"],
            fontName="DocMono",
            fontSize=6.8,
            leading=9,
            textColor=colors.HexColor("#0F172A"),
            leftIndent=7,
            rightIndent=7,
            spaceBefore=5,
            spaceAfter=7,
            backColor=PALE,
            borderColor=LINE,
            borderWidth=0.5,
            borderPadding=7,
        ),
        "caption": ParagraphStyle(
            "Caption",
            parent=base["BodyText"],
            fontName="DocSans-Oblique",
            fontSize=7.3,
            leading=10,
            textColor=MID,
            alignment=TA_CENTER,
            spaceAfter=6,
        ),
    }
    return styles


class ProjectDocTemplate(BaseDocTemplate):
    def __init__(self, filename: str, **kwargs):
        super().__init__(filename, **kwargs)

    def afterFlowable(self, flowable: Flowable) -> None:
        outline_level = getattr(flowable, "_outline_level", None)
        toc_level = getattr(flowable, "_toc_level", None)
        if outline_level is None and toc_level is None:
            return
        text = flowable.getPlainText()
        key = getattr(flowable, "_bookmark_key", None)
        if key is None:
            key = f"heading-{id(flowable)}"
            flowable._bookmark_key = key
        self.canv.bookmarkPage(key)
        if outline_level is not None and outline_level <= 1:
            self.canv.addOutlineEntry(text, key, level=outline_level, closed=False)
        if toc_level is not None:
            self.notify("TOCEntry", (toc_level, text, self.page, key))


def header_footer(canvas, doc) -> None:
    canvas.saveState()
    page_w, page_h = doc.pagesize
    canvas.setStrokeColor(LINE)
    canvas.setLineWidth(0.5)
    canvas.line(18 * mm, page_h - 14 * mm, page_w - 18 * mm, page_h - 14 * mm)
    canvas.setFont("DocSans", 7.2)
    canvas.setFillColor(MID)
    canvas.drawString(18 * mm, page_h - 10 * mm, "Sistema POS e inventario - Minimarket familiar")
    canvas.drawRightString(page_w - 18 * mm, 10 * mm, f"Pagina {doc.page}")
    canvas.restoreState()


def cover_page(canvas, doc) -> None:
    canvas.saveState()
    page_w, page_h = A4
    canvas.setFillColor(NAVY)
    canvas.rect(0, 0, page_w, page_h, fill=1, stroke=0)
    canvas.setFillColor(BLUE)
    canvas.rect(0, 0, 14 * mm, page_h, fill=1, stroke=0)
    canvas.setFillColor(colors.HexColor("#1E3A8A"))
    canvas.circle(page_w - 12 * mm, page_h - 25 * mm, 45 * mm, fill=1, stroke=0)
    canvas.restoreState()


def heading(text: str, level: int, styles) -> Paragraph:
    style = styles[f"h{min(level, 3)}"]
    item = Paragraph(inline_markup(text), style)
    item._outline_level = min(level - 1, 2)
    if level == 1:
        item._toc_level = 0
    return item


def table_widths(columns: int, available: float) -> list[float]:
    ratios = {
        2: [0.30, 0.70],
        3: [0.22, 0.25, 0.53],
        4: [0.16, 0.22, 0.30, 0.32],
        5: [0.12, 0.14, 0.23, 0.33, 0.18],
        6: [0.10, 0.10, 0.22, 0.11, 0.35, 0.12],
    }.get(columns, [1 / columns] * columns)
    return [available * ratio for ratio in ratios]


def markdown_table(rows: list[list[str]], styles, wide: bool = False) -> LongTable:
    columns = len(rows[0])
    page_w = landscape(A4)[0] if wide else A4[0]
    available = page_w - 36 * mm
    cell_style = styles["cell_small"] if wide or columns >= 5 else styles["cell"]
    data = []
    for row_index, row in enumerate(rows):
        style = styles["cell_head"] if row_index == 0 else cell_style
        padded = row + [""] * (columns - len(row))
        data.append([Paragraph(inline_markup(cell), style) for cell in padded[:columns]])
    widths = table_widths(columns, available)
    if columns == 4 and rows[0][0] == "ID / Pri.":
        widths = [available * ratio for ratio in (0.13, 0.30, 0.19, 0.38)]
    elif columns == 4 and rows[0][0] == "Regla":
        widths = [available * ratio for ratio in (0.15, 0.39, 0.29, 0.17)]
    elif columns == 4 and rows[0][:3] == ["ID", "Riesgo", "Exposición"]:
        widths = [available * ratio for ratio in (0.10, 0.30, 0.17, 0.43)]
    table = LongTable(data, colWidths=widths, repeatRows=1, hAlign="LEFT")
    commands = [
        ("BACKGROUND", (0, 0), (-1, 0), NAVY),
        ("TEXTCOLOR", (0, 0), (-1, 0), WHITE),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]
    for index in range(1, len(data)):
        if index % 2 == 0:
            commands.append(("BACKGROUND", (0, index), (-1, index), PALE))
    table.setStyle(TableStyle(commands))
    return table


def parse_mermaid_nodes(lines: list[str]):
    nodes: dict[str, str] = {}
    edges: list[tuple[str, str, str]] = []
    node_re = re.compile(r'(\w+)\["([^"]+)"\]')
    edge_re = re.compile(
        r'(\w+)(?:\["[^"]+"\])?\s*-->\s*(?:\|"?([^|"]+)"?\|\s*)?(\w+)(?:\["[^"]+"\])?'
    )
    for line in lines:
        for match in node_re.finditer(line):
            nodes[match.group(1)] = clean_text(match.group(2))
        match = edge_re.search(line.strip())
        if match:
            source, label, target = match.groups()
            nodes.setdefault(source, source.replace("_", " ").title())
            nodes.setdefault(target, target.replace("_", " ").title())
            edges.append((source, target, clean_text(label or "")))
    return nodes, edges


def split_label(label: str, max_chars: int = 22) -> list[str]:
    words = label.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = f"{current} {word}".strip()
        if current and len(candidate) > max_chars:
            lines.append(current)
            current = word
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines[:3]


def flow_diagram(lines: list[str]) -> Drawing:
    nodes, edges = parse_mermaid_nodes(lines)
    node_ids = list(nodes)
    indegree = {node: 0 for node in node_ids}
    outgoing = {node: [] for node in node_ids}
    for source, target, _ in edges:
        indegree[target] = indegree.get(target, 0) + 1
        outgoing.setdefault(source, []).append(target)
    queue = [node for node in node_ids if indegree.get(node, 0) == 0]
    rank = {node: 0 for node in queue}
    seen = set()
    while queue:
        node = queue.pop(0)
        seen.add(node)
        for target in outgoing.get(node, []):
            rank[target] = max(rank.get(target, 0), rank[node] + 1)
            indegree[target] -= 1
            if indegree[target] == 0:
                queue.append(target)
    for index, node in enumerate(node_ids):
        rank.setdefault(node, index // 3)
    levels: dict[int, list[str]] = {}
    for node in node_ids:
        levels.setdefault(rank[node], []).append(node)
    width = 455
    node_h = 38
    gap_y = 42
    height = max(110, 30 + len(levels) * (node_h + gap_y))
    drawing = Drawing(width, height)
    positions: dict[str, tuple[float, float, float]] = {}
    for level_index, level in sorted(levels.items()):
        count = len(level)
        gap_x = 12
        node_w = min(125, (width - 30 - gap_x * max(0, count - 1)) / max(1, count))
        total = count * node_w + (count - 1) * gap_x
        start_x = (width - total) / 2
        y = height - 25 - level_index * (node_h + gap_y) - node_h
        for item_index, node in enumerate(level):
            x = start_x + item_index * (node_w + gap_x)
            positions[node] = (x, y, node_w)
    for source, target, label in edges:
        if source not in positions or target not in positions:
            continue
        sx, sy, sw = positions[source]
        tx, ty, tw = positions[target]
        x1, y1 = sx + sw / 2, sy
        x2, y2 = tx + tw / 2, ty + node_h
        drawing.add(Line(x1, y1, x2, y2, strokeColor=MID, strokeWidth=0.9))
        angle = math.atan2(y2 - y1, x2 - x1)
        arrow = 4
        p1 = (x2, y2)
        p2 = (x2 - arrow * math.cos(angle - 0.55), y2 - arrow * math.sin(angle - 0.55))
        p3 = (x2 - arrow * math.cos(angle + 0.55), y2 - arrow * math.sin(angle + 0.55))
        drawing.add(Polygon([*p1, *p2, *p3], fillColor=MID, strokeColor=MID))
        if label:
            drawing.add(String((x1 + x2) / 2 + 3, (y1 + y2) / 2 + 3, label, fontName="DocSans", fontSize=5.8, fillColor=MID))
    for node, (x, y, node_w) in positions.items():
        drawing.add(Rect(x, y, node_w, node_h, rx=6, ry=6, fillColor=LIGHT_BLUE, strokeColor=BLUE, strokeWidth=0.8))
        label_lines = split_label(nodes[node], 24)
        start_y = y + node_h / 2 + (len(label_lines) - 1) * 4
        for index, label in enumerate(label_lines):
            drawing.add(String(x + node_w / 2, start_y - index * 9, label, textAnchor="middle", fontName="DocSans-Bold", fontSize=6.7, fillColor=NAVY))
    return drawing


def er_table(lines: list[str], styles) -> Table:
    relation_re = re.compile(r"(\w+)\s+([^\s]+)\s+(\w+)\s*:\s*(.+)")
    rows = [["Entidad A", "Cardinalidad", "Relación", "Entidad B"]]
    for line in lines:
        match = relation_re.match(line.strip())
        if match:
            left, cardinality, right, relation = match.groups()
            rows.append([left, cardinality, clean_text(relation), right])
    return markdown_table(rows, styles, wide=False)


def parse_markdown(path: Path, styles, include_h1: bool = True) -> list[Flowable]:
    lines = clean_text(path.read_text(encoding="utf-8")).splitlines()
    story: list[Flowable] = []
    index = 0
    first_heading = True
    while index < len(lines):
        line = lines[index].rstrip()
        stripped = line.strip()
        if not stripped:
            index += 1
            continue
        if stripped.startswith("```"):
            language = stripped[3:].strip().lower()
            index += 1
            block: list[str] = []
            while index < len(lines) and not lines[index].strip().startswith("```"):
                block.append(lines[index])
                index += 1
            index += 1
            if language == "mermaid":
                meaningful = [value for value in block if value.strip()]
                if meaningful and meaningful[0].strip().startswith("erDiagram"):
                    story.extend([Spacer(1, 4), er_table(meaningful[1:], styles), Paragraph("Relaciones del modelo de dominio", styles["caption"])])
                else:
                    diagram_lines = meaningful[1:] if meaningful and meaningful[0].strip().startswith("flowchart") else meaningful
                    story.extend([Spacer(1, 4), flow_diagram(diagram_lines), Paragraph("Diagrama arquitectónico", styles["caption"])])
            else:
                wrapped = []
                for code_line in block:
                    if len(code_line) <= 92:
                        wrapped.append(code_line)
                    else:
                        wrapped.extend(code_line[i:i + 92] for i in range(0, len(code_line), 92))
                story.append(Preformatted("\n".join(wrapped), styles["code"]))
            continue
        if stripped.startswith("|") and index + 1 < len(lines) and re.match(r"^\s*\|?\s*:?-+", lines[index + 1]):
            raw_rows: list[list[str]] = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                cells = [cell.strip() for cell in lines[index].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", cell.replace(" ", "")) for cell in cells):
                    raw_rows.append(cells)
                index += 1
            wide = len(raw_rows[0]) >= 5
            if wide:
                preceding_heading = None
                if story and isinstance(story[-1], Paragraph) and hasattr(story[-1], "_outline_level"):
                    preceding_heading = story.pop()
                story.extend([NextPageTemplate("landscape"), PageBreak()])
                if preceding_heading is not None:
                    story.append(preceding_heading)
                story.extend([markdown_table(raw_rows, styles, wide=True), NextPageTemplate("portrait"), PageBreak()])
            else:
                story.extend([Spacer(1, 4), markdown_table(raw_rows, styles), Spacer(1, 6)])
            continue
        heading_match = re.match(r"^(#{1,6})\s+(.+)$", stripped)
        if heading_match:
            level = len(heading_match.group(1))
            title = heading_match.group(2)
            if level == 1 and first_heading and not include_h1:
                first_heading = False
                index += 1
                continue
            story.append(heading(title, min(level, 3), styles))
            first_heading = False
            index += 1
            continue
        bullet_match = re.match(r"^\s*[-*]\s+(.+)$", line)
        if bullet_match:
            story.append(Paragraph(inline_markup(bullet_match.group(1)), styles["bullet"], bulletText="•"))
            index += 1
            continue
        number_match = re.match(r"^\s*(\d+)\.\s+(.+)$", line)
        if number_match:
            story.append(Paragraph(inline_markup(number_match.group(2)), styles["number"], bulletText=f"{number_match.group(1)}."))
            index += 1
            continue
        if stripped in {"---", "***"}:
            story.append(HRFlowable(width="100%", thickness=0.6, color=LINE, spaceBefore=6, spaceAfter=6))
            index += 1
            continue
        paragraph_lines = [stripped]
        index += 1
        while index < len(lines):
            candidate = lines[index].strip()
            if not candidate:
                index += 1
                break
            if candidate.startswith(("#", "```", "|", "- ", "* ")) or re.match(r"^\d+\.\s+", candidate):
                break
            paragraph_lines.append(candidate)
            index += 1
        text = " ".join(paragraph_lines)
        style = styles["meta"] if text.startswith("**Proyecto:**") or text.startswith("**Estado:**") or text.startswith("**Versión:**") or text.startswith("**Fecha:**") else styles["body"]
        story.append(Paragraph(inline_markup(text), style))
    return story


def build_pdf() -> Path:
    register_fonts()
    styles = build_styles()
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    portrait_size = A4
    landscape_size = landscape(A4)
    portrait_frame = Frame(18 * mm, 16 * mm, portrait_size[0] - 36 * mm, portrait_size[1] - 33 * mm, id="portrait-frame")
    landscape_frame = Frame(18 * mm, 16 * mm, landscape_size[0] - 36 * mm, landscape_size[1] - 33 * mm, id="landscape-frame")
    cover_frame = Frame(28 * mm, 28 * mm, portrait_size[0] - 56 * mm, portrait_size[1] - 56 * mm, id="cover-frame", showBoundary=0)

    doc = ProjectDocTemplate(
        str(OUTPUT),
        pagesize=A4,
        title="Sistema POS e inventario para minimarket familiar",
        author="Proyecto técnico",
        subject="Documentación aprobada - Entregas 1 y 2",
        leftMargin=18 * mm,
        rightMargin=18 * mm,
        topMargin=18 * mm,
        bottomMargin=16 * mm,
    )
    doc.addPageTemplates([
        PageTemplate(id="cover", pagesize=A4, frames=[cover_frame], onPage=cover_page),
        PageTemplate(id="portrait", pagesize=A4, frames=[portrait_frame], onPage=header_footer),
        PageTemplate(id="landscape", pagesize=landscape_size, frames=[landscape_frame], onPage=header_footer),
    ])

    story: list[Flowable] = [
        Spacer(1, 58 * mm),
        Paragraph("SISTEMA POS E INVENTARIO", ParagraphStyle("CoverKicker", fontName="DocSans-Bold", fontSize=12, leading=16, textColor=colors.HexColor("#93C5FD"), alignment=TA_LEFT, spaceAfter=8)),
        Paragraph("Minimarket familiar", ParagraphStyle("CoverTitle", fontName="DocSans-Bold", fontSize=30, leading=36, textColor=WHITE, alignment=TA_LEFT, spaceAfter=14)),
        Paragraph("Documentación aprobada - Entregas 1 y 2", ParagraphStyle("CoverSub", fontName="DocSans", fontSize=15, leading=21, textColor=colors.HexColor("#DCE8FF"), alignment=TA_LEFT, spaceAfter=24)),
        HRFlowable(width="58%", thickness=2, color=BLUE, spaceBefore=4, spaceAfter=18, hAlign="LEFT"),
        Paragraph("Alcance funcional, dominio, arquitectura, seguridad y decisiones técnicas", ParagraphStyle("CoverDesc", fontName="DocSans", fontSize=10.5, leading=16, textColor=WHITE, alignment=TA_LEFT, spaceAfter=18)),
        Paragraph("Restricción vigente: recursos locales o planes gratuitos; ningún cobro sin aprobación explícita.", ParagraphStyle("CoverRule", fontName="DocSans-Bold", fontSize=9.2, leading=14, textColor=colors.HexColor("#BFDBFE"), alignment=TA_LEFT, borderColor=colors.HexColor("#3B82F6"), borderWidth=0.8, borderPadding=8, backColor=colors.HexColor("#1E3A5F"))),
        Spacer(1, 42 * mm),
        Paragraph("Versión documental 0.2 | 31 de julio de 2026", ParagraphStyle("CoverFoot", fontName="DocSans", fontSize=8, textColor=colors.HexColor("#CBD5E1"))),
        NextPageTemplate("portrait"),
        PageBreak(),
        Paragraph("Contenido", styles["h1"]),
    ]

    toc = TableOfContents()
    toc.levelStyles = [
        ParagraphStyle("TOC1", fontName="DocSans-Bold", fontSize=9.5, leading=14, textColor=NAVY, leftIndent=0, firstLineIndent=0, spaceBefore=4),
        ParagraphStyle("TOC2", fontName="DocSans", fontSize=8.2, leading=12, textColor=TEXT, leftIndent=12, firstLineIndent=0),
        ParagraphStyle("TOC3", fontName="DocSans", fontSize=7.4, leading=10, textColor=MID, leftIndent=24, firstLineIndent=0),
    ]
    story.extend([toc, PageBreak()])

    for doc_path in DOCS:
        story.extend(parse_markdown(doc_path, styles))
        story.append(PageBreak())

    story.extend([
        Paragraph("ANEXO", ParagraphStyle("AnnexKicker", fontName="DocSans-Bold", fontSize=11, textColor=BLUE, spaceAfter=7)),
        heading("Registro de decisiones arquitectónicas", 1, styles),
        Paragraph("Los siguientes ADR conservan el contexto, la decisión, sus consecuencias y las alternativas descartadas. Todos fueron aceptados al aprobar las Entregas 1 y 2.", styles["lead"]),
        PageBreak(),
    ])
    for adr_path in ADRS:
        story.extend(parse_markdown(adr_path, styles))
        story.append(PageBreak())

    story.extend([
        heading("Cierre documental", 1, styles),
        Paragraph("Las Entregas 1 y 2 quedan aprobadas. La implementación permanece bloqueada hasta aprobar la Entrega 3: estrategia de pruebas, QA, quality gates, regresión, liberación, gestión de defectos y backlog ejecutable.", styles["lead"]),
    ])

    doc.multiBuild(story)
    return OUTPUT


if __name__ == "__main__":
    print(build_pdf())
