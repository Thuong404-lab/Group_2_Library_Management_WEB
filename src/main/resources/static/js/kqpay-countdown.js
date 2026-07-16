(function () {
    "use strict";

    document.querySelectorAll("[data-kqpay-countdown]").forEach(function (countdown) {
        const expiresAt = Number(countdown.dataset.expiresAt || 0);
        const time = countdown.querySelector("[data-kqpay-time]");
        if (!expiresAt || time == null) return;

        let timer = null;
        function update() {
            const remainingSeconds = Math.max(0, Math.ceil((expiresAt - Date.now()) / 1000));
            const minutes = Math.floor(remainingSeconds / 60);
            const seconds = remainingSeconds % 60;
            time.textContent = String(minutes).padStart(2, "0") + ":" + String(seconds).padStart(2, "0");

            if (remainingSeconds === 0) {
                countdown.classList.add("is-expired");
                const label = countdown.querySelector("[data-kqpay-label]");
                if (label != null) label.textContent = "Mã QR đã hết hạn";
                if (timer != null) clearInterval(timer);
            }

            return remainingSeconds > 0;
        }

        if (update()) timer = setInterval(update, 1000);
    });
})();
