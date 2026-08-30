# Perkz Android App

Perkz is an Android app that pulls your credit-card perks from a Google Sheet (CSV export), shows them by interval, and lets you check them off per period.

## Features

- Imports perks from a Google Sheet CSV URL.
- Groups perks by interval (monthly, quarterly, yearly, etc.).
- Shows each perk's value/uses from your **Max Value / Uses** column.
- Lets you mark each perk as used for its current period.
- Can sync checkbox changes back to Google Sheets using an Apps Script webhook.
- Saves check-off status locally on your phone.

## Sheet format

Use a header row with these columns (case-insensitive):

- `name` / `title` / `perk` / `benefit`
- `card` / `card name`
- `interval` / `frequency` / `cadence`
- `max value / uses`
- `notes` / `details` / `description`

If headers are missing, the app uses the first columns in this order:
title, card, interval, details.

## Setup

The app requires you to provide your Google Sheet CSV export URL. On first launch, you'll be prompted to enter it in the Settings tab.

### Getting your CSV export URL

1. Open your Google Sheet
2. Go to **File** → **Download** → **Comma-separated values (.csv)**
3. Copy the URL from your browser (or manually construct: `https://docs.google.com/spreadsheets/d/{SHEET_ID}/export?format=csv&gid={GID}`)
4. Paste into app Settings and tap **Save settings**

If your sheet is private, make sure it is shared so the URL can be accessed.

## Run in Android Studio

1. Open this folder in Android Studio.
2. Let Gradle sync finish.
3. Run the app on your Android phone or emulator.
4. Save your sheet URL in the app and tap **Refresh**.

## Enable write-back to Google Sheet

To make checkbox toggles update your sheet, you need to create a Google Apps Script webhook. Here's how:

### Step 1: Create a new Apps Script project

1. Open [script.google.com](https://script.google.com)
2. Click **+ New project**
3. Name it something like "Perkz Webhook"
4. Get your **Sheet ID** from your Google Sheet URL:
   - URL format: `https://docs.google.com/spreadsheets/d/{SHEET_ID}/edit`
   - Copy just the `{SHEET_ID}` part
5. In the `Code.gs` file, replace all code with this (add your sheet ID):

```javascript
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
  if (idxUsed) sheet.getRange(row, idxUsed).setValue(data.checked ? "yes" : "");

  return ContentService.createTextOutput(JSON.stringify({ ok: true }))
    .setMimeType(ContentService.MimeType.JSON);
}
```

### Step 2: Deploy as Web App

1. Click **Deploy** button in the top right
2. If this is a new project:
   - Select **New deployment**
   - Click the gear/settings icon
   - Choose **Web app**
   - Set **Execute as**: Your Google account
   - Set **Who has access**: Anyone with the link
   - Click **Deploy**
3. If you already have deployments:
   - Click **Deploy** → **Manage deployments**
   - Or click the dropdown next to Deploy and select the existing deployment
4. A dialog will show your deployment URL - copy it
5. Click **Authorize access** and grant permission for the script to access your sheets

### Step 3: Add webhook URL to Perkz app

1. Open the Perkz app
2. Go to **Settings** tab
3. Paste the webhook URL into **"Update webhook URL (Apps Script)"**
4. Click **Save settings**

Now when you check/uncheck perks, they'll automatically update in your Google Sheet!

### Your Google Sheet must have these columns:
- **Date Used** - Where the app records when a perk was used (required)
- **Used** - Optional, cleared when perk is marked as unused

## Troubleshooting

### Checkbox clicks do nothing
1. **Check Settings** - Go to Settings tab and verify both URLs are entered
   - Sheet URL should contain `/export?format=csv`
   - Webhook URL should be your Apps Script deployment URL
2. **Check error messages** - Look for red error text at bottom of screen
3. **Verify sheet columns** - Make sure your Google Sheet has **Date Used** column
4. **Check Apps Script sheet ID** - In Apps Script, verify `ALLOWED_SHEET_ID` matches your actual sheet

### Checkbox changes locally but doesn't sync to Sheet
1. **Webhook URL might be incorrect** - Double-check it's the full deployment URL
2. **Sheet ID mismatch** - The sheet ID in Apps Script must match your actual sheet
3. **Authorization issue** - Make sure the Apps Script is authorized to access your sheet
4. **Permissions** - Your sheet must be writable by the account that deployed the Apps Script

### How to verify your setup
1. **Get your Sheet ID** from the URL: `docs.google.com/spreadsheets/d/{SHEET_ID}/edit`
2. **Copy exact deployment URL** from Apps Script deploy dialog
3. **Test the webhook** by checking a perk - you should see:
   - Checkbox visually changes
   - Snackbar message appears (success or error)
   - Sheet updates within a few seconds
4. **Check Google Sheet** - Verify the "Date Used" column has today's date

### Still not working?
- Make sure your Google Sheet columns are exactly: `Date Used` (case-insensitive is OK)
- Verify Apps Script says `Deployed as: Web app`
- Try **Save settings** again in the app after double-checking URLs
