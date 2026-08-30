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
4. In the `Code.gs` file, replace all code with this:

```javascript
function doPost(e) {
  const data = JSON.parse(e.postData.contents || "{}");
  const sheet = SpreadsheetApp.openById(data.sheetId).getSheetByName(
    SpreadsheetApp.openById(data.sheetId).getSheets().find(s => String(s.getSheetId()) === String(data.gid)).getName()
  );

  const header = sheet.getRange(1, 1, 1, sheet.getLastColumn()).getValues()[0];
  const idxDateUsed = header.findIndex(h => String(h).trim().toLowerCase() === "date used") + 1;
  const idxUsed = header.findIndex(h => String(h).trim().toLowerCase() === "used") + 1;
  if (!idxDateUsed) throw new Error("Missing 'Date Used' column");

  const row = Number(data.rowNumber);
  sheet.getRange(row, idxDateUsed).setValue(data.checked ? data.dateUsed : "");
  if (idxUsed && !data.checked) sheet.getRange(row, idxUsed).setValue("");

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
