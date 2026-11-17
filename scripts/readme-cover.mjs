#!/usr/bin/env node
// Draw the README cover: three schema versions whose fields converge into one wrapper, beside the wordmark.
// Writes docs/images/cover.svg and renders docs/images/cover.png (2560x1280) with Chrome.
//
// Usage: node scripts/readme-cover.mjs

import { writeFileSync, mkdtempSync, rmSync, existsSync, mkdirSync } from 'node:fs';
import { execFileSync } from 'node:child_process';
import { join, resolve, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { tmpdir } from 'node:os';

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const CHROME = process.env.CHROME || '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome';
const W = 1280, H = 640;

const STYLE = `
  <style>
    text { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; }
    .word { fill: #e0e0e0; font-size: 80px; font-weight: 700; letter-spacing: 0.14em; }
    .sub { fill: #2dd4bf; font-size: 20px; font-weight: 500; letter-spacing: 0.12em; }
    .rule { stroke: #2a2a2a; stroke-width: 1; }
    .ver { fill: #6b6b6b; font: 500 14px ui-monospace, Menlo, monospace; }
  </style>`;

const WORDMARK = `
  <text class="word" x="86" y="262">PROTO</text>
  <text class="word" x="86" y="352">WRAPPER</text>
  <line class="rule" x1="92" y1="386" x2="590" y2="386"/>
  <text class="sub" x="92" y="424">ONE JAVA API FOR EVERY SCHEMA VERSION</text>`;

// Fields of the message; the wrapper has all of them.
const NAMES = [44, 30, 52, 38, 46, 34, 40, 48, 28];
// Field 4 is int32 in v1 and an enum later: the type conflict the wrapper resolves.
const CONFLICT = 4;
const VERSIONS = [
    { label: 'v1', fields: [0, 1, 2, 3, 4, 5, 6], enumAt: false },
    { label: 'v2', fields: [0, 1, 2, 3, 4, 5, 7], enumAt: true },
    { label: 'v3', fields: [0, 1, 2, 3, 4, 5, 7, 8], enumAt: true },
];

function marker(x, y, s, fill, diamond, extra = '') {
    const t = diamond ? ` transform="rotate(45 ${x} ${y})"` : '';
    return `  <rect x="${x - s / 2}" y="${y - s / 2}" width="${s}" height="${s}" rx="1.5" fill="${fill}"${t}${extra}/>`;
}

function cover() {
    const out = [], links = [];
    // Version cards: compact, stacked on the left of the picture.
    const vx = 700, vw = 100, vpitch = 12, vpad = 10, vh = NAMES.length * vpitch + 2 * vpad;
    // Wrapper card: larger, on the right.
    const wx = 1045, ww = 150, wpitch = 30, wpad = 20, wh = NAMES.length * wpitch + 2 * wpad;
    const wy = (H - wh) / 2;
    const wrow = i => wy + wpad + wpitch * (i + 0.5);

    VERSIONS.forEach((v, k) => {
        const y0 = 112 + k * (vh + 16);
        out.push(`  <rect x="${vx}" y="${y0}" width="${vw}" height="${vh}" rx="6" fill="#161616" stroke="#2e2e2e" stroke-width="1.2"/>`);
        out.push(`  <text class="ver" x="${vx - 12}" y="${y0 + vh / 2 + 5}" text-anchor="end">${v.label}</text>`);
        NAMES.forEach((len, i) => {
            const y = y0 + vpad + vpitch * (i + 0.5);
            const bx = vx + 22, bw = len * 1.3;
            if (!v.fields.includes(i)) {
                out.push(`  <rect x="${bx - 12.5}" y="${y - 3.5}" width="7" height="7" rx="1.5" fill="none" stroke="#333" stroke-dasharray="2 2"/>`);
                out.push(`  <rect x="${bx}" y="${y - 2}" width="${bw}" height="4" rx="2" fill="none" stroke="#2c2c2c" stroke-dasharray="3 3"/>`);
                return;
            }
            const conflict = i === CONFLICT;
            out.push(marker(bx - 9, y, 7, conflict ? '#0d9488' : '#4a4a4a', conflict && v.enumAt));
            out.push(`  <rect x="${bx}" y="${y - 2}" width="${bw}" height="4" rx="2" fill="#2f2f2f"/>`);
            const x1 = vx + vw, x2 = wx, y2 = wrow(i), mx = (x1 + x2) / 2;
            const stroke = conflict ? '#14b8a6' : '#3a3a3a';
            const op = conflict ? 0.75 : 0.8;
            links.push(`  <path d="M${x1} ${y.toFixed(1)} C${mx} ${y.toFixed(1)} ${mx} ${y2.toFixed(1)} ${x2} ${y2.toFixed(1)}" fill="none" stroke="${stroke}" stroke-width="1" opacity="${op}"/>`);
        });
    });

    const wrapper = [`  <rect x="${wx}" y="${wy}" width="${ww}" height="${wh}" rx="8" fill="#0c2321" stroke="#14b8a6" stroke-width="1.5"/>`];
    NAMES.forEach((len, i) => {
        const y = wrow(i), conflict = i === CONFLICT;
        wrapper.push(marker(wx + 26, y, 12, conflict ? '#99f6e4' : '#2dd4bf', false));
        wrapper.push(`  <rect x="${wx + 42}" y="${y - 3.5}" width="${len * 1.9}" height="7" rx="3.5" fill="#14b8a6" opacity="${conflict ? 0.9 : 0.55}"/>`);
    });

    const defs = `
    <radialGradient id="glow" cx="50%" cy="50%" r="50%">
      <stop offset="0" stop-color="#14b8a6" stop-opacity="0.22"/>
      <stop offset="1" stop-color="#14b8a6" stop-opacity="0"/>
    </radialGradient>`;
    const glow = `  <ellipse cx="${wx + ww / 2}" cy="${H / 2}" rx="220" ry="240" fill="url(#glow)"/>`;
    return wrap([glow, ...links, ...out, ...wrapper].join('\n') + '\n', defs);
}

function wrap(body, defs = '') {
    return `<svg width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg">${STYLE}
  <defs>${defs}</defs>
  <rect width="${W}" height="${H}" fill="#0f0f0f"/>
${body}${WORDMARK}
</svg>
`;
}

function screenshot(url, width, height, png, profile) {
    execFileSync(CHROME, [
        '--headless=new', '--disable-gpu', '--hide-scrollbars',
        ...(process.env.CI ? ['--no-sandbox'] : []),
        `--user-data-dir=${profile}`,
        '--force-device-scale-factor=2',
        `--window-size=${width},${height}`,
        `--screenshot=${png}`,
        url,
    ], { stdio: 'ignore' });
    if (!existsSync(png)) throw new Error(`Chrome wrote no ${png}`);
}

const dir = join(ROOT, 'docs', 'images');
mkdirSync(dir, { recursive: true });
const svg = cover();
writeFileSync(join(dir, 'cover.svg'), svg);

const work = mkdtempSync(join(tmpdir(), 'proto-wrapper-cover-'));
try {
    const html = join(work, 'cover.html');
    writeFileSync(html, `<!doctype html><meta charset="utf-8"><style>html,body{margin:0;background:#0f0f0f}svg{display:block}</style>${svg}`);
    const png = join(dir, 'cover.png');
    rmSync(png, { force: true });
    screenshot(`file://${html}`, W, H, png, join(work, 'chrome'));
} finally {
    rmSync(work, { recursive: true, force: true });
}
console.log(`wrote ${join(dir, 'cover.svg')} and cover.png`);
