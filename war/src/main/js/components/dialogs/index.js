import { createElementFromHtml } from "@/util/dom";
import { CLOSE } from "@/util/symbols";

var _defaults = {
  title: null,
  message: null,
  cancel: false,
  okText: "OK",
  cancelText: "Cancel",
  maxWidth: "475px",
  minWidth: "",
  type: "default",
  hideCloseButton: false,
  allowEmpty: false,
};

var _typeClassMap = {
  default: "",
  destructive: "jenkins-!-destructive-color",
};

function Dialog(dialogType, options) {
  this.dialogType = dialogType;
  this.options = Object.assign({}, _defaults, options);
  this.init();
}

Dialog.prototype.init = function () {
  this.dialog = document.createElement("dialog");
  this.dialog.classList.add("jenkins-dialog");
  this.dialog.style.maxWidth = this.options.maxWidth;
  this.dialog.style.maxWidth = this.options.minWidth;

  let contentStyle = "jenkins-dialog__contents";
  if (this.options.title != null) {
    const title = createElementFromHtml(`<div class='jenkins-dialog__title'/>`);
    this.dialog.appendChild(title);
    title.innerText = this.options.title;
    contentStyle = "jenkins-dialog__contents_title";
  }

  if (this.dialogType === "modal") {
    if (this.options.content != null) {
      const content = createElementFromHtml(`<div class='${contentStyle}'/>`);
      content.appendChild(this.options.content);
      this.dialog.appendChild(content);
    }
    if (this.options.hideCloseButton !== true) {
      const closeButton = createElementFromHtml(`
          <button class="jenkins-dialog__close-button jenkins-button">
            <span class="jenkins-visually-hidden">Close</span>
            ${CLOSE}
          </button>
        `);
      this.dialog.appendChild(closeButton);
      closeButton.addEventListener("click", () =>
        this.dialog.dispatchEvent(new Event("cancel"))
      );
    }
    this.dialog.addEventListener("click", function (e) {
      if (e.target !== e.currentTarget) {
        return;
      }
      this.dispatchEvent(new Event("cancel"));
    });
    this.ok = null;
  } else {
    if (this.options.message != null) {
      const message = createElementFromHtml(`<div class='${contentStyle}'/>`);
      this.dialog.appendChild(message);
      message.innerText = this.options.message;
    }

    if (this.dialogType === "prompt") {
      let inputDiv = createElementFromHtml(`<div class="jenkins-dialog__input">
          <input data-id="input" type="text" class='jenkins-input'></div>`);
      this.dialog.appendChild(inputDiv);
      this.input = inputDiv.querySelector("[data-id=input]");
      if (!this.options.allowEmpty) {
        this.input.addEventListener("input", () => this.checkInput());
      }
    }

    this.appendButtons();

    this.dialog.addEventListener("keydown", (e) => {
      if (e.key === "Enter") {
        e.preventDefault();
        if (this.ok.disabled == false) {
          this.ok.dispatchEvent(new Event("click"));
        }
      }
      if (e.key === "Escape") {
        e.preventDefault();
        this.dialog.dispatchEvent(new Event("cancel"));
      }
    });
  }
  document.body.appendChild(this.dialog);
};

Dialog.prototype.checkInput = function () {
  if (this.input.value.trim()) {
    this.ok.disabled = false;
  } else {
    this.ok.disabled = true;
  }
};

Dialog.prototype.appendButtons = function () {
  const buttons = createElementFromHtml(`<div
      class="jenkins-buttons-row jenkins-buttons-row--equal-width jenkins-dialog__buttons">
      <button data-id="ok" class="jenkins-button jenkins-button--primary ${
        _typeClassMap[this.options.type]
      }">${this.options.okText}</button>
      <button data-id="cancel" class="jenkins-button">${
        this.options.cancelText
      }</button>
    </div>`);

  this.dialog.appendChild(buttons);

  this.ok = buttons.querySelector("[data-id=ok]");
  this.cancel = buttons.querySelector("[data-id=cancel]");
  if (!this.options.cancel) {
    this.cancel.style.display = "none";
  } else {
    this.cancel.addEventListener("click", (e) => {
      e.preventDefault();
      this.dialog.dispatchEvent(new Event("cancel"));
    });
  }
  if (this.dialogType === "prompt" && !this.options.allowEmpty) {
    this.ok.disabled = true;
  }
};

Dialog.prototype.show = function () {
  return new Promise((resolve, cancel) => {
    this.dialog.showModal();
    this.dialog.addEventListener(
      "cancel",
      (e) => {
        e.preventDefault();
        this.dialog.addEventListener("webkitAnimationEnd", () => {
          this.dialog.remove();
          cancel();
        });
        this.dialog.classList.add("jenkins-dialog--hidden");
      },
      { once: true }
    );
    this.dialog.focus();
    if (this.input != null) {
      this.input.focus();
    }
    if (this.ok != null) {
      this.ok.addEventListener(
        "click",
        (e) => {
          e.preventDefault();

          let value = true;
          if (this.dialogType === "prompt") {
            value = this.input.value;
          }
          this.dialog.addEventListener("webkitAnimationEnd", () => {
            this.dialog.remove();
            resolve(value);
          });
          this.dialog.classList.add("jenkins-dialog--hidden");
        },
        { once: true }
      );
    }
  });
};

function init() {
  window.dialog = {
    modal: function (content, options) {
      const defaults = {
        content: content,
        ok: false,
      };
      options = Object.assign({}, defaults, options);
      let dialog = new Dialog("modal", options);
      dialog
        .show()
        .then()
        .catch(() => {});
    },

    alert: function (message, options) {
      const defaults = {
        message: message,
      };
      options = Object.assign({}, defaults, options);
      let dialog = new Dialog("alert", options);
      dialog
        .show()
        .then()
        .catch(() => {});
    },

    confirm: function (message, options) {
      const defaults = {
        message: message,
        okText: "Yes",
        cancel: true,
      };
      options = Object.assign({}, defaults, options);
      let dialog = new Dialog("confirm", options);
      return dialog.show();
    },

    prompt: function (message, options) {
      const defaults = {
        message: message,
        minWidth: "400px",
        cancel: true,
      };
      options = Object.assign({}, defaults, options);
      let dialog = new Dialog("prompt", options);
      return dialog.show();
    },
  };
}

export default { init };
