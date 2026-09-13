const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACTIVE_JOB_KEY = "convertformat.activeJob";
const MODES = {
    document: {
        accept: ".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        extensions: [".docx"],
        dropTitle: "DOCX dosyanı buraya bırak",
        submitTitle: "PDF’e dönüştür",
        outputExtension: ".pdf",
        readyTitle: "PDF’in hazır",
        downloadTitle: "PDF’i indir"
    },
    image: {
        accept: ".jpg,.jpeg,.png,image/jpeg,image/png",
        extensions: [".jpg", ".jpeg", ".png"],
        dropTitle: "JPEG veya PNG görselini buraya bırak",
        submitTitle: "WebP’ye dönüştür",
        outputExtension: ".webp",
        readyTitle: "WebP görselin hazır",
        downloadTitle: "WebP’yi indir"
    }
};

const form = document.querySelector("#upload-form");
const fileInput = document.querySelector("#file-input");
const dropZone = document.querySelector("#drop-zone");
const selectedFile = document.querySelector("#selected-file");
const fileName = document.querySelector("#file-name");
const fileSize = document.querySelector("#file-size");
const removeFileButton = document.querySelector("#remove-file");
const submitButton = document.querySelector("#submit-button");
const modeButtons = document.querySelectorAll(".format-option");
const dropTitle = document.querySelector("#drop-title");
const qualityControl = document.querySelector("#quality-control");
const qualityInput = document.querySelector("#quality");
const qualityValue = document.querySelector("#quality-value");
const resultPanel = document.querySelector("#result-panel");
const statusIndicator = document.querySelector("#status-indicator");
const resultTitle = document.querySelector("#result-title");
const resultMessage = document.querySelector("#result-message");
const downloadButton = document.querySelector("#download-button");
const newConversionButton = document.querySelector("#new-conversion");

let selectedDocument = null;
let pollingTimer = null;
let activeJob = restoreActiveJob();
let activeMode = "document";

fileInput.addEventListener("change", () => selectFile(fileInput.files[0]));
modeButtons.forEach((button) => button.addEventListener("click", () => switchMode(button.dataset.mode)));
qualityInput.addEventListener("input", () => qualityValue.textContent = qualityInput.value);
removeFileButton.addEventListener("click", resetSelection);
newConversionButton.addEventListener("click", resetAll);
downloadButton.addEventListener("click", downloadPdf);
form.addEventListener("submit", startConversion);

["dragenter", "dragover"].forEach((eventName) => {
    dropZone.addEventListener(eventName, (event) => {
        event.preventDefault();
        dropZone.classList.add("is-dragging");
    });
});

["dragleave", "drop"].forEach((eventName) => {
    dropZone.addEventListener(eventName, (event) => {
        event.preventDefault();
        dropZone.classList.remove("is-dragging");
    });
});

dropZone.addEventListener("drop", (event) => selectFile(event.dataTransfer.files[0]));

if (activeJob) {
    showProcessing("Dönüşüm kontrol ediliyor", activeJob.originalFileName);
    pollStatus();
}

function selectFile(file) {
    if (!file) return;

    const mode = MODES[activeMode];
    const normalizedFileName = file.name.toLowerCase();
    const hasAllowedExtension = mode.extensions.some((extension) => normalizedFileName.endsWith(extension));

    if (!hasAllowedExtension) {
        resetSelection();
        showError(
            "Geçersiz dosya",
            activeMode === "document"
                ? "Lütfen .docx uzantılı bir belge seç."
                : "Lütfen .jpg, .jpeg veya .png uzantılı bir görsel seç."
        );
        return;
    }

    if (file.size > MAX_FILE_SIZE) {
        resetSelection();
        showError("Dosya çok büyük", "DOCX dosyası en fazla 10 MB olabilir.");
        return;
    }

    selectedDocument = file;
    fileName.textContent = file.name;
    fileSize.textContent = formatBytes(file.size);
    selectedFile.hidden = false;
    resultPanel.hidden = true;
    submitButton.disabled = false;
}

function switchMode(modeName) {
    if (!MODES[modeName] || modeName === activeMode) return;

    activeMode = modeName;
    const mode = MODES[activeMode];

    modeButtons.forEach((button) => {
        const isActive = button.dataset.mode === activeMode;
        button.classList.toggle("is-active", isActive);
        button.setAttribute("aria-selected", String(isActive));
    });

    fileInput.accept = mode.accept;
    dropTitle.textContent = mode.dropTitle;
    submitButton.textContent = mode.submitTitle;
    qualityControl.hidden = activeMode !== "image";
    resultPanel.hidden = true;
    resetSelection();
}

function resetSelection() {
    selectedDocument = null;
    fileInput.value = "";
    selectedFile.hidden = true;
    submitButton.disabled = true;
}

function resetAll() {
    clearTimeout(pollingTimer);
    sessionStorage.removeItem(ACTIVE_JOB_KEY);
    activeJob = null;
    form.hidden = false;
    resultPanel.hidden = true;
    resetSelection();
}

async function startConversion(event) {
    event.preventDefault();
    if (!selectedDocument) return;

    submitButton.disabled = true;
    submitButton.textContent = "Yükleniyor…";

    const body = new FormData();
    body.append("file", selectedDocument);
    if (activeMode === "image") {
        body.append("quality", qualityInput.value);
    }

    try {
        const response = await fetch("/api/v1/conversions", {method: "POST", body});
        const payload = await readJson(response);

        if (!response.ok) {
            throw new Error(payload.message || "Dosya yüklenemedi.");
        }

        activeJob = {
            id: payload.id,
            downloadToken: payload.downloadToken,
            originalFileName: payload.originalFileName,
            conversionType: payload.conversionType
        };
        sessionStorage.setItem(ACTIVE_JOB_KEY, JSON.stringify(activeJob));
        form.hidden = true;
        showProcessing("Dosyan dönüştürülüyor", payload.originalFileName);
        pollStatus();
    } catch (error) {
        showError("Yükleme başarısız", error.message);
        submitButton.disabled = false;
    } finally {
        submitButton.textContent = MODES[activeMode].submitTitle;
    }
}

async function pollStatus() {
    if (!activeJob) return;

    try {
        const response = await fetch(`/api/v1/conversions/${activeJob.id}`);
        const payload = await readJson(response);

        if (!response.ok) {
            throw new Error(payload.message || "İşlem durumu alınamadı.");
        }

        if (payload.status === "COMPLETED") {
            showComplete();
            return;
        }

        if (payload.status === "FAILED") {
            showError("Dönüşüm tamamlanamadı", payload.errorMessage || "Lütfen tekrar dene.");
            newConversionButton.hidden = false;
            return;
        }

        resultMessage.textContent = payload.status === "PROCESSING"
            ? "Belge PDF formatına aktarılıyor…"
            : "Dönüşüm sırasına alındı…";
        pollingTimer = setTimeout(pollStatus, 1200);
    } catch (error) {
        resultMessage.textContent = `${error.message} Tekrar deneniyor…`;
        pollingTimer = setTimeout(pollStatus, 2500);
    }
}

async function downloadPdf() {
    if (!activeJob) return;

    downloadButton.disabled = true;
    downloadButton.textContent = "İndiriliyor…";

    try {
        const response = await fetch(`/api/v1/conversions/${activeJob.id}/file`, {
            headers: {"X-Download-Token": activeJob.downloadToken}
        });

        if (!response.ok) {
            const payload = await readJson(response);
            throw new Error(payload.message || "PDF indirilemedi.");
        }

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = outputFileName(activeJob);
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    } catch (error) {
        resultMessage.textContent = error.message;
    } finally {
        downloadButton.disabled = false;
        downloadButton.textContent = activeJob?.conversionType === "IMAGE_TO_WEBP"
            ? MODES.image.downloadTitle
            : MODES.document.downloadTitle;
    }
}

function showProcessing(title, message) {
    resultPanel.hidden = false;
    statusIndicator.className = "status-indicator";
    resultTitle.textContent = title;
    resultMessage.textContent = message;
    downloadButton.hidden = true;
    newConversionButton.hidden = true;
}

function showComplete() {
    statusIndicator.className = "status-indicator is-complete";
    const isImage = activeJob.conversionType === "IMAGE_TO_WEBP";
    resultTitle.textContent = isImage ? MODES.image.readyTitle : MODES.document.readyTitle;
    resultMessage.textContent = outputFileName(activeJob);
    downloadButton.textContent = isImage ? MODES.image.downloadTitle : MODES.document.downloadTitle;
    downloadButton.hidden = false;
    newConversionButton.hidden = false;
}

function outputFileName(job) {
    const extension = job.conversionType === "IMAGE_TO_WEBP"
        ? MODES.image.outputExtension
        : MODES.document.outputExtension;
    const lastDot = job.originalFileName.lastIndexOf(".");
    const baseName = lastDot < 0 ? job.originalFileName : job.originalFileName.slice(0, lastDot);
    return `${baseName}${extension}`;
}

function showError(title, message) {
    resultPanel.hidden = false;
    statusIndicator.className = "status-indicator is-error";
    resultTitle.textContent = title;
    resultMessage.textContent = message;
    downloadButton.hidden = true;
}

function restoreActiveJob() {
    try {
        return JSON.parse(sessionStorage.getItem(ACTIVE_JOB_KEY));
    } catch {
        sessionStorage.removeItem(ACTIVE_JOB_KEY);
        return null;
    }
}

async function readJson(response) {
    const contentType = response.headers.get("content-type") || "";
    return contentType.includes("application/json") ? response.json() : {};
}

function formatBytes(bytes) {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
