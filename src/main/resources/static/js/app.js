const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACTIVE_JOB_KEY = "convertformat.activeJob";
const MODES = {
    document: {
        conversionType: "DOCX_TO_PDF",
        accept: ".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        extensions: [".docx"],
        invalidMessage: "Lütfen .docx uzantılı bir belge seç.",
        dropTitle: "DOCX dosyanı buraya bırak",
        submitTitle: "PDF’e dönüştür",
        outputExtension: ".pdf",
        readyTitle: "PDF’in hazır",
        downloadTitle: "PDF’i indir"
    },
    imageToWebp: {
        conversionType: "IMAGE_TO_WEBP",
        accept: ".jpg,.jpeg,.png,image/jpeg,image/png",
        extensions: [".jpg", ".jpeg", ".png"],
        invalidMessage: "Lütfen .jpg, .jpeg veya .png uzantılı bir görsel seç.",
        dropTitle: "JPEG veya PNG görselini buraya bırak",
        submitTitle: "WebP’ye dönüştür",
        outputExtension: ".webp",
        readyTitle: "WebP görselin hazır",
        downloadTitle: "WebP’yi indir",
        qualityLabel: "WebP kalitesi",
        defaultQuality: 82
    },
    webpToJpeg: {
        conversionType: "WEBP_TO_JPEG",
        accept: ".webp,image/webp",
        extensions: [".webp"],
        invalidMessage: "Lütfen .webp uzantılı bir görsel seç.",
        dropTitle: "WebP görselini buraya bırak",
        submitTitle: "JPG’ye dönüştür",
        outputExtension: ".jpg",
        readyTitle: "JPG görselin hazır",
        downloadTitle: "JPG’yi indir",
        qualityLabel: "JPG kalitesi",
        defaultQuality: 90
    },
    webpToPng: {
        conversionType: "WEBP_TO_PNG",
        accept: ".webp,image/webp",
        extensions: [".webp"],
        invalidMessage: "Lütfen .webp uzantılı bir görsel seç.",
        dropTitle: "WebP görselini buraya bırak",
        submitTitle: "PNG’ye dönüştür",
        outputExtension: ".png",
        readyTitle: "PNG görselin hazır",
        downloadTitle: "PNG’yi indir"
    },
    imageToPdf: {
        conversionType: "IMAGE_TO_PDF",
        accept: ".jpg,.jpeg,.png,image/jpeg,image/png",
        extensions: [".jpg", ".jpeg", ".png"],
        invalidMessage: "Lütfen .jpg, .jpeg veya .png uzantılı bir görsel seç.",
        dropTitle: "JPEG veya PNG görselini buraya bırak",
        submitTitle: "PDF’e dönüştür",
        outputExtension: ".pdf",
        readyTitle: "PDF’in hazır",
        downloadTitle: "PDF’i indir"
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
const modeSelect = document.querySelector("#conversion-mode");
const dropTitle = document.querySelector("#drop-title");
const qualityControl = document.querySelector("#quality-control");
const qualityInput = document.querySelector("#quality");
const qualityValue = document.querySelector("#quality-value");
const qualityLabel = document.querySelector("#quality-label");
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
modeSelect.addEventListener("change", () => switchMode(modeSelect.value));
qualityInput.addEventListener("input", () => qualityValue.textContent = qualityInput.value);
removeFileButton.addEventListener("click", resetSelection);
newConversionButton.addEventListener("click", resetAll);
downloadButton.addEventListener("click", downloadResult);
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
        showError("Geçersiz dosya", mode.invalidMessage);
        return;
    }

    if (file.size > MAX_FILE_SIZE) {
        resetSelection();
        showError("Dosya çok büyük", "Dosya en fazla 10 MB olabilir.");
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

    fileInput.accept = mode.accept;
    dropTitle.textContent = mode.dropTitle;
    submitButton.textContent = mode.submitTitle;
    qualityControl.hidden = mode.defaultQuality === undefined;
    if (mode.defaultQuality !== undefined) {
        qualityInput.value = mode.defaultQuality;
        qualityValue.textContent = mode.defaultQuality;
        qualityLabel.textContent = mode.qualityLabel;
    }
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
    body.append("conversionType", MODES[activeMode].conversionType);
    if (MODES[activeMode].defaultQuality !== undefined) {
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
            ? "Dosya hedef formata dönüştürülüyor…"
            : "Dönüşüm sırasına alındı…";
        pollingTimer = setTimeout(pollStatus, 1200);
    } catch (error) {
        resultMessage.textContent = `${error.message} Tekrar deneniyor…`;
        pollingTimer = setTimeout(pollStatus, 2500);
    }
}

async function downloadResult() {
    if (!activeJob) return;

    downloadButton.disabled = true;
    downloadButton.textContent = "İndiriliyor…";

    try {
        const response = await fetch(`/api/v1/conversions/${activeJob.id}/file`, {
            headers: {"X-Download-Token": activeJob.downloadToken}
        });

        if (!response.ok) {
            const payload = await readJson(response);
            throw new Error(payload.message || "Dönüştürülen dosya indirilemedi.");
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
        downloadButton.textContent = modeForConversionType(activeJob?.conversionType).downloadTitle;
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
    const mode = modeForConversionType(activeJob.conversionType);
    resultTitle.textContent = mode.readyTitle;
    resultMessage.textContent = outputFileName(activeJob);
    downloadButton.textContent = mode.downloadTitle;
    downloadButton.hidden = false;
    newConversionButton.hidden = false;
}

function outputFileName(job) {
    const extension = modeForConversionType(job.conversionType).outputExtension;
    const lastDot = job.originalFileName.lastIndexOf(".");
    const baseName = lastDot < 0 ? job.originalFileName : job.originalFileName.slice(0, lastDot);
    return `${baseName}${extension}`;
}

function modeForConversionType(conversionType) {
    return Object.values(MODES).find((mode) => mode.conversionType === conversionType)
        || MODES.document;
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
