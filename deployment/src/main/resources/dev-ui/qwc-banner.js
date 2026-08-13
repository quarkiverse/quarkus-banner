import { LitElement, html, css } from 'lit';
import { unsafeHTML } from 'lit/directives/unsafe-html.js';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/text-field';
import '@vaadin/combo-box';
import '@vaadin/checkbox';
import '@vaadin/button';
import '@vaadin/icon';
import { notifier } from 'notifier';
import { fonts, colors, defaults } from 'build-time-data';

/**
 * Dev UI card page for the Quarkus Banner extension: preview the banner with any bundled font and text, and
 * print it to the running application's console — no restart, no config edit.
 */
export class QwcBanner extends LitElement {

    jsonRpc = new JsonRpc(this);

    static styles = css`
        :host {
            display: flex;
            flex-direction: column;
            gap: 15px;
            padding: 15px;
            height: 100%;
        }
        .controls {
            display: flex;
            flex-wrap: wrap;
            align-items: flex-end;
            gap: 15px;
        }
        .text {
            flex: 1 1 260px;
        }
        .font {
            flex: 0 0 240px;
        }
        .color {
            flex: 0 0 180px;
        }
        .preview {
            flex: 1;
            margin: 0;
            padding: 15px;
            overflow: auto;
            background: var(--lumo-contrast-5pct);
            border: 1px solid var(--lumo-contrast-10pct);
            border-radius: var(--lumo-border-radius-m);
            font-family: monospace;
            font-size: 12px;
            line-height: 1.1;
            white-space: pre;
            color: var(--lumo-body-text-color);
        }
        .error {
            color: var(--lumo-error-text-color);
        }
    `;

    static properties = {
        _text: { state: true },
        _font: { state: true },
        _powerBy: { state: true },
        _color: { state: true },
        _backgroundColor: { state: true },
        _banner: { state: true },
        _error: { state: true },
    };

    constructor() {
        super();
        this._text = defaults.text;
        this._font = defaults.font;
        this._powerBy = defaults.powerBy;
        this._color = defaults.color;
        this._backgroundColor = defaults.backgroundColor;
        this._banner = '';
        this._error = '';
    }

    connectedCallback() {
        super.connectedCallback();
        this._refresh();
    }

    render() {
        return html`
            <div class="controls">
                <vaadin-text-field class="text" label="Text" .value="${this._text}"
                    @value-changed="${(e) => { this._text = e.detail.value; this._refresh(); }}"></vaadin-text-field>
                <vaadin-combo-box class="font" label="Font" .items="${fonts}" .value="${this._font}"
                    @value-changed="${(e) => { this._font = e.detail.value; this._refresh(); }}"></vaadin-combo-box>
                <vaadin-combo-box class="color" label="Colour" allow-custom-value helper-text="name or #rrggbb"
                    .items="${colors}" item-label-path="label" item-value-path="value" .value="${this._color}"
                    @value-changed="${(e) => { this._color = e.detail.value; this._refresh(); }}"></vaadin-combo-box>
                <vaadin-combo-box class="color" label="Background" allow-custom-value helper-text="name or #rrggbb"
                    .items="${colors}" item-label-path="label" item-value-path="value" .value="${this._backgroundColor}"
                    @value-changed="${(e) => { this._backgroundColor = e.detail.value; this._refresh(); }}"></vaadin-combo-box>
                <vaadin-checkbox label="Powered by Quarkus" ?checked="${this._powerBy}"
                    @checked-changed="${(e) => { this._powerBy = e.detail.value; this._refresh(); }}"></vaadin-checkbox>
                <vaadin-button theme="primary" @click="${this._print}">
                    <vaadin-icon icon="font-awesome-solid:terminal" slot="prefix"></vaadin-icon>
                    Print to log
                </vaadin-button>
            </div>
            ${this._error
                ? html`<div class="error">${this._error}</div>`
                : html`<pre class="preview">${unsafeHTML(this._ansiToHtml(this._banner))}</pre>`}
        `;
    }

    _params() {
        return {
            text: this._text, font: this._font, powerBy: this._powerBy,
            color: this._color, backgroundColor: this._backgroundColor,
        };
    }

    _refresh() {
        this.jsonRpc.render(this._params()).then((response) => this._apply(response.result));
    }

    // Renders the ANSI-coloured banner as styled HTML spans for the preview (the console gets the raw ANSI).
    _ansiToHtml(text) {
        const FG = {
            30: '#000000', 31: '#cd0000', 32: '#00cd00', 33: '#cdcd00', 34: '#2222ee', 35: '#cd00cd',
            36: '#00cdcd', 37: '#e5e5e5', 90: '#7f7f7f', 91: '#ff0000', 92: '#00ff00', 93: '#ffff00',
            94: '#5c5cff', 95: '#ff00ff', 96: '#00ffff', 97: '#ffffff',
        };
        const BG = {};
        Object.keys(FG).forEach((k) => { BG[Number(k) + 10] = FG[k]; });
        const escape = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

        let fg = null;
        let bg = null;
        const span = (chunk) => {
            const styles = [];
            if (fg) styles.push(`color:${fg}`);
            if (bg) styles.push(`background:${bg}`);
            return styles.length ? `<span style="${styles.join(';')}">${escape(chunk)}</span>` : escape(chunk);
        };

        let out = '';
        let last = 0;
        const re = /\x1b\[([0-9;]*)m/g;
        let match;
        while ((match = re.exec(text)) !== null) {
            if (match.index > last) out += span(text.slice(last, match.index));
            const codes = match[1].split(';').filter((c) => c.length).map(Number);
            if (codes.length === 0) { fg = null; bg = null; }
            for (let k = 0; k < codes.length; k++) {
                const c = codes[k];
                if (c === 0) { fg = null; bg = null; }
                else if ((c === 38 || c === 48) && codes[k + 1] === 2) {
                    const rgb = `rgb(${codes[k + 2] || 0},${codes[k + 3] || 0},${codes[k + 4] || 0})`;
                    if (c === 38) fg = rgb; else bg = rgb;
                    k += 4;
                }
                else if (FG[c]) fg = FG[c];
                else if (BG[c]) bg = BG[c];
            }
            last = re.lastIndex;
        }
        if (last < text.length) out += span(text.slice(last));
        return out;
    }

    _print() {
        this.jsonRpc.display(this._params())
            .then((response) => {
                this._apply(response.result);
                if (!response.result.error) {
                    notifier.showInfoMessage('Banner printed to the application console.');
                }
            });
    }

    _apply(result) {
        if (result.error) {
            this._error = result.error;
            this._banner = '';
        } else {
            this._error = '';
            this._banner = result.banner;
        }
    }
}

customElements.define('qwc-banner', QwcBanner);
