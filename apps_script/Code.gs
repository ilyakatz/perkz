// IMPORTANT: Replace with YOUR Google Sheet ID
const ALLOWED_SHEET_ID = "YOUR_SHEET_ID_HERE";

function doPost(e) {
  const data = JSON.parse(e.postData.contents || "{}");
  
  // Security: Only allow updates to your specific sheet
  if (data.sheetId !== ALLOWED_SHEET_ID) {
    return ContentService.createTextOutput(JSON.stringify({ 
      error: "Unauthorized sheet" 
    })).setMimeType(ContentService.MimeType.JSON);
  }
  
  const sheet = SpreadsheetApp.openById(data.sheetId).getSheetByName(
    SpreadsheetApp.openById(data.sheetId).getSheets().find(s => String(s.getSheetId()) === String(data.gid)).getName()
  );

  const header = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  const idxDateUsed = header.findIndex(h => String(h).trim().toLowerCase() === "date used") + 1;
  const idxUsed = header.findIndex(h => String(h).trim().toLowerCase() === "used") + 1;
  if (!idxDateUsed) throw new Error("Missing 'Date Used' column");

  const row = Number(data.rowNumber);
  sheet.getRange(row, idxDateUsed).setValue(data.checked ? data.dateUsed : "");
  if (idxUsed) sheet.getRange(row, idxUsed).setValue(data.checked ? data.usedValue : "");

  return ContentService.createTextOutput(JSON.stringify({ ok: true }))
    .setMimeType(ContentService.MimeType.JSON);
}