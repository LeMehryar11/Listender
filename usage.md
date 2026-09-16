# Usage Manual

This guide outlines user workflows, navigation models, and backup mechanics within Listender.

---

## 1. Primary Bottom Navigation

Listender is partitioned into three focused views accessible via the bottom navigation bar:
- **Tasks:** Today, Weekly, Monthly, and Yearly habit checklists with interactive star scoring, multi-scope aggregation, and scoped observations/reflections.
- **Reports:** Performance analytics featuring interactive completion trend line graphs across Days, Weeks, Months, and Years with bidirectional window navigation and swipe gestures, as well as current scope breakdowns.
- **Settings:** Quick interface appearance selection (Light / Dark / System), first day of the week preferences, JSON Backup & Restore with collision management, and privacy information.

---

## 2. Managing Tasks & Habits

- **Adding a Task:** Tap the Floating Action Button (`+`) anchored in the lower right corner of the Tasks screen.
- **Temporal Scopes:** Switch smoothly between `Today`, `Weekly`, `Monthly`, and `Yearly` tabs spanning the full screen width.
- **Period Navigation:** Tap `<` or `>` to move backward or forward across days, weeks, months, or years.
- **Present Jump:** Tap `Present` to return immediately to the current period (the button is greyed out when already on the present date).
- **Previous Period Import:** When entering a period, the **Import from the day/week/month/year before** button appears if unimported tasks exist. Tapping it checks task names to avoid importing duplicate tasks into the current period.

---

## 3. Star Progression Mechanics

- Each task specifies a **target star count** (e.g. 5 stars for daily hydration).
- Tap any star directly to set your earned score, or tap an active star to decrement.
- **Silver Stars:** Displayed whilst progress is below the designated target.
- **Gold Stars:** Illuminated automatically once the target threshold is reached or exceeded.
- **Overachievement:** Extra badges indicate progress achieved beyond the display bounds.
- **Hierarchical Aggregation:** Weekly summaries aggregate both weekly and daily tasks; Monthly summaries aggregate monthly, weekly, and daily tasks; Yearly summaries aggregate across all four tiers.

---

## 4. Re-ordering & Managing Task Periods

- **Re-ordering:** Long-press and drag any task item via its drag handle to adjust sequence in the list.
- **Editing & Star Adjustment:** Tap any task card or select **Edit Task** from the card menu to adjust the title, scope, or star numbers. You can simply type the desired number of **Target Stars** and **Achieved Stars** directly, or use the adjacent stepper buttons. Achieved stars cannot exceed target stars without increasing the target count first.
- **Move to Next Period:** Choose **Move to tomorrow / the next week / month / year** from the task menu to reschedule the task into the following period.
- **Move Across Scopes (Today / Current Week / Month / Year):** From any temporal scope, the task menu allows moving the task directly to any of the other three current periods (e.g., from Daily to the current week, month, or year; from Weekly to today, current month, or year; etc.).
- **Move to Current Period:** When viewing tasks outside the current period within the same scope, the menu also provides an option to return the task to the present period (today / current week / current month / current year).
- **Deletion:** Choose **Delete Task** from the menu to remove the task.

---

## 5. Performance Analytics & Trend Graphs

1. Tap **Reports** in the bottom navigation bar.
2. Select among the four trend intervals: **Days**, **Weeks**, **Months**, or **Years**.
3. Use the `<` and `>` arrow controls or horizontally drag/swipe across the graph card to pull older or newer periods into view.
4. Tap any data point on the line curve to inspect completion rates, earned stars, and target totals for that point.
5. Tap **Back to Present** to return instantly to current date windows.
6. Review the **Current Scope Breakdown** progress cards below the graph.

---

## 6. Settings & Backup / Restore

1. Tap **Settings** in the bottom navigation bar.
2. **Appearance:** Select Light, Dark, or System mode.
3. **First Day of the Week:** Select Monday, Sunday, or Saturday.
4. **Backup & Restore:**
   - **Export JSON:** Exports complete recorded tasks, star achievements, and period notes in structured JSON format via the system share sheet.
   - **Import JSON:** Loads JSON backup files and offers collision resolution options (*Merge Progress*, *Skip Existing*, or *Overwrite Existing*).
5. **Privacy & Architecture:** Complete local-only offline persistence details.
