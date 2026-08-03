import { LitElement, html, css } from 'lit';
import { JsonRpc } from 'jsonrpc';
import '@vaadin/text-field';
import '@vaadin/combo-box';
import '@vaadin/checkbox';
import '@vaadin/button';
import '@vaadin/icon';
import { notifier } from 'notifier';
import { fonts, defaults } from 'build-time-data';

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
        _banner: { state: true },
        _error: { state: true },
    };

    constructor() {
        super();
        this._text = defaults.text;
        this._font = defaults.font;
        this._powerBy = defaults.powerBy;
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
                <vaadin-checkbox label="Powered by Quarkus" ?checked="${this._powerBy}"
                    @checked-changed="${(e) => { this._powerBy = e.detail.value; this._refresh(); }}"></vaadin-checkbox>
                <vaadin-button theme="primary" @click="${this._print}">
                    <vaadin-icon icon="font-awesome-solid:terminal" slot="prefix"></vaadin-icon>
                    Print to log
                </vaadin-button>
            </div>
            ${this._error
                ? html`<div class="error">${this._error}</div>`
                : html`<pre class="preview">${this._banner}</pre>`}
        `;
    }

    _refresh() {
        this.jsonRpc.render({ text: this._text, font: this._font, powerBy: this._powerBy })
            .then((response) => this._apply(response.result));
    }

    _print() {
        this.jsonRpc.display({ text: this._text, font: this._font, powerBy: this._powerBy })
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
