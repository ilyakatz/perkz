// IMPORTANT: Replace with YOUR Google Sheet ID
const ALLOWED_SHEET_ID = "YOUR_SHEET_ID_HERE";

function doPost(e) {
  let data;
  try {
    data = JSON.parse(e.postData.contents || "{}");
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ error: "Invalid JSON" })).setMimeType(ContentService.MimeType.JSON);
  }
  
  if (data.sheetId !== ALLOWED_SHEET_ID) {
    return ContentService.createTextOutput(JSON.stringify({ error: "Unauthorized" })).setMimeType(ContentService.MimeType.JSON);
  }
  
  const ss = SpreadsheetApp.openById(data.sheetId);
  const sheet = ss.getSheetByName(ss.getSheets().find(s => String(s.getSheetId()) === String(data.gid || "0")).getName());
  const header = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];

  if (data.action === "append") {
    return handleAppend(sheet, header, data);
  } else {
    return handleUpdate(sheet, header, data);
  }
}

function handleUpdate(sheet, header, data) {
  const updates = data.updates || {};
  const row = Number(data.rowNumber);
  let count = 0;

  for (const idx in updates) {
    const colIndex = Number(idx) + 1;
    if (colIndex > 0 && colIndex <= header.length) {
      sheet.getRange(row, colIndex).setValue(updates[idx]);
      count++;
    }
  }
  return ContentService.createTextOutput(JSON.stringify({ ok: count > 0 })).setMimeType(ContentService.MimeType.JSON);
}

function handleAppend(sheet, header, data) {
  const updates = data.updates || {};
  const newRow = new Array(header.length).fill("");
  let hasData = false;

  for (const idx in updates) {
    const colIndex = Number(idx);
    if (colIndex >= 0 && colIndex < header.length) {
      newRow[colIndex] = updates[idx];
      hasData = true;
    }
  }

  if (hasData) {
    sheet.appendRow(newRow);
  }
  return ContentService.createTextOutput(JSON.stringify({ ok: hasData, rowNumber: sheet.getLastRow() })).setMimeType(ContentService.MimeType.JSON);
}
