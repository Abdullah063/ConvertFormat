const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACTIVE_JOB_KEY = "convertformat.activeJob";

const form = document.querySelector("#upload-form");
const fileInput = document.querySelector("#file-input");
const dropZone = document.querySelector("#drop-zone");
const selectedFile = document.querySelector("#selected-file");
const fileName = document.querySelector("#file-name");
const fileSize = document.querySelector("#file-size");
const removeFileButton = document.querySelector("#remove-file");
const submitButton = document.querySelector("#submit-button");
const resultPanel = document.querySelector("#result-panel");
const statusIndicator = document.querySelector("#status-indicator");
const resultTitle = document.querySelector("#result-title");
const resultMessage = document.querySelector("#result-message");
const downloadButton = document.querySelector("#download-button");
const newConversionButton = document.querySelector("#new-conversion");

let selectedDocument = null;
let pollingTimer = null;
let activeJob = restoreActiveJob();

fileInput.addEventListener("change", () => selectFile(fileInput.files[0]));
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

    if (!file.name.toLowerCase().endsWith(".docx")) {
        resetSelection();
        showError("Geçersiz dosya", "Lütfen .docx uzantılı bir belge seç.");
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

    try {
        const response = await fetch("/api/v1/conversions", {method: "POST", body});
        const payload = await readJson(response);

        if (!response.ok) {
            throw new Error(payload.message || "Dosya yüklenemedi.");
        }

        activeJob = {
            id: payload.id,
            downloadToken: payload.downloadToken,
            originalFileName: payload.originalFileName
        };
        sessionStorage.setItem(ACTIVE_JOB_KEY, JSON.stringify(activeJob));
        form.hidden = true;
        showProcessing("Dosyan dönüştürülüyor", payload.originalFileName);
        pollStatus();
    } catch (error) {
        showError("Yükleme başarısız", error.message);
        submitButton.disabled = false;
    } finally {
        submitButton.textContent = "PDF’e dönüştür";
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
        link.download = activeJob.originalFileName.replace(/\.docx$/i, ".pdf");
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    } catch (error) {
        resultMessage.textContent = error.message;
    } finally {
        downloadButton.disabled = false;
        downloadButton.textContent = "PDF’i indir";
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
    resultTitle.textContent = "PDF’in hazır";
    resultMessage.textContent = activeJob.originalFileName.replace(/\.docx$/i, ".pdf");
    downloadButton.hidden = false;
    newConversionButton.hidden = false;
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
