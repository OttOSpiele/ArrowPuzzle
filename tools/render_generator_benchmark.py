#!/usr/bin/env python3
"""Render exact CI-generated Arrow Puzzle boards to a human-readable PDF.

Usage: python tools/render_generator_benchmark.py game-core/build/benchmark
Requires reportlab. Two boards per landscape A4 page.
"""
import csv
import math
import sys
from pathlib import Path
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.pdfgen import canvas


def draw_board(pdf, row, x, y, width, height):
    stage = int(row["stage"])
    seed = int(row["seed"])
    cols = int(row["width"])
    rows = int(row["height"])
    fallback = row["fallback"].lower() == "true"
    paths = [
        [tuple(map(int, point.split(":"))) for point in path.split(";")]
        for path in row["paths"].split("|") if path
    ]
    pdf.setStrokeColor(colors.HexColor("#DBE3EF"))
    pdf.setLineWidth(0.7)
    pdf.roundRect(x, y, width, height, 9, fill=0, stroke=1)
    pdf.setFillColor(colors.HexColor("#26354E"))
    pdf.setFont("Helvetica-Bold", 13)
    pdf.drawString(x + 14, y + height - 24, f"Stufe {stage}   Seed {seed}")
    pdf.setFont("Helvetica", 9)
    kind = "FALLBACK (sandfarbenes Raster)" if fallback else "Normal (blaugraues Raster)"
    pdf.setFillColor(colors.HexColor("#966B32") if fallback else colors.HexColor("#60738B"))
    pdf.drawRightString(x + width - 12, y + height - 23, kind)
    pdf.setFillColor(colors.HexColor("#5F6E84"))
    pdf.drawString(x + 14, y + height - 40,
        f"{cols} x {rows} | {len(paths)} Pfeile | Belegung {float(row['occupancy_pct']):.1f}%"
        f" | Frei {row['free']} | Kette {row['chain']} | Score {float(row['quality']):.1f}")
    usable_w = width - 35
    usable_h = height - 82
    cell = min(usable_w / cols, usable_h / rows)
    bx = x + (width - cell * cols) / 2
    by = y + 15 + (usable_h - cell * rows) / 2
    dotcolor = colors.HexColor("#E4C58D" if fallback else "#CED7ED")
    pdf.setFillColor(dotcolor)
    for gy in range(rows):
        for gx in range(cols):
            pdf.circle(bx + (gx + .5) * cell, by + (rows - gy - .5) * cell,
                       max(.55, cell * .10), fill=1, stroke=0)
    pdf.setLineCap(1)
    pdf.setLineJoin(1)
    pdf.setStrokeColor(colors.HexColor("#202632"))
    pdf.setFillColor(colors.HexColor("#202632"))
    pdf.setLineWidth(max(1.5, cell * .26))
    for vertices in paths:
        if len(vertices) < 2:
            continue
        points = [(bx + (px + .5) * cell, by + (rows - py - .5) * cell)
                  for px, py in vertices]
        p = pdf.beginPath()
        p.moveTo(*points[0])
        for point in points[1:]:
            p.lineTo(*point)
        pdf.drawPath(p, stroke=1)
        (x0, y0), (x1, y1) = points[-2:]
        angle = math.atan2(y1 - y0, x1 - x0)
        length = max(3, cell * .60)
        spread = length * .55
        triangle = [
            (x1 + math.cos(angle)*length*.25, y1 + math.sin(angle)*length*.25),
            (x1 - math.cos(angle)*length + math.sin(angle)*spread,
             y1 - math.sin(angle)*length - math.cos(angle)*spread),
            (x1 - math.cos(angle)*length - math.sin(angle)*spread,
             y1 - math.sin(angle)*length + math.cos(angle)*spread),
        ]
        head = pdf.beginPath()
        head.moveTo(*triangle[0])
        for point in triangle[1:]:
            head.lineTo(*point)
        head.close()
        pdf.drawPath(head, fill=1, stroke=0)


def main():
    folder = Path(sys.argv[1] if len(sys.argv) > 1 else "game-core/build/benchmark")
    input_name = sys.argv[2] if len(sys.argv) > 2 else "boards.tsv"
    output_name = sys.argv[3] if len(sys.argv) > 3 else "generated-levels.pdf"
    with (folder / input_name).open(encoding="utf-8", newline="") as fp:
        boards = list(csv.DictReader(fp, delimiter="\t"))
    if not boards:
        raise SystemExit("No boards in benchmark geometry export")
    pdf_path = folder / output_name
    pdf = canvas.Canvas(str(pdf_path), pagesize=landscape(A4), pageCompression=1)
    page_w, page_h = landscape(A4)
    margin = 22
    gutter = 14
    tile_w = (page_w - 2*margin - gutter) / 2
    tile_h = page_h - 72
    for offset in range(0, len(boards), 2):
        page_index = offset//2 + 1
        pdf.setTitle("Arrow Puzzle - " + output_name)
        pdf.setFillColor(colors.HexColor("#202B3D"))
        pdf.setFont("Helvetica-Bold", 16)
        pdf.drawString(margin, page_h - 27, "Arrow Puzzle  |  " + ("Exact-Cover-Level" if "exact-cover" in output_name else "Generator-Referenzlevel"))
        pdf.setFont("Helvetica", 9)
        pdf.setFillColor(colors.HexColor("#758399"))
        pdf.drawRightString(page_w-margin, page_h-25,
                            f"Seite {page_index} / {math.ceil(len(boards)/2)}")
        draw_board(pdf, boards[offset], margin, 17, tile_w, tile_h)
        if offset+1 < len(boards):
            draw_board(pdf, boards[offset+1],
                       margin+tile_w+gutter, 17, tile_w, tile_h)
        pdf.showPage()
    pdf.save()
    print(f"Rendered {len(boards)} boards -> {pdf_path}")


if __name__ == "__main__":
    main()
