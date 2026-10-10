# Demo walkthrough

A guided tour of the app using the seeded data. Start the app (see the README) and sign in as `admin` / `admin123`
unless a step says otherwise. If you changed the seed passwords, use those.

The seeded data: two projects (Green Valley Layout and Riverside Enclave) with eight plots each; Vikram Sharma on
an installment plan (down payment and first installment paid); Ananya Sen who paid in full and whose plot is sold
(2% commission credited to `agent`); and two open leads, Rohan Verma and Priya Nair.

## 1. Sign-up and roles
1. Open `/register` and create an account, for example Full name `David Miller`, username `david`, role **Agent**.
   Only Agent and Accountant are offered, because admins are never self-registered.
2. Sign in as `david`. Notice that Ledger (payments), Installments and Audit Log are not in the menu, since agents do not have access.
3. Sign out and sign back in as `admin`.

## 2. Projects and inventory
1. Open **Projects**. Add `Emerald Palms Estate`, location `Devenahalli, Bengaluru`.
2. Open **Inventory** and add a plot: project `Emerald Palms Estate`, block `A`, area `1500`, facing `East`,
   price `2000000`. It starts as `AVAILABLE`.
3. The delete button works only for `AVAILABLE` or `CANCELLED` plots.

## 3. Capture a lead
1. Open **Leads** and add `Karan Malhotra`, phone `+91 99880 11223`, project `Emerald Palms Estate`, source
   `WEBSITE`, assigned agent `Alex Agent (agent)`, with a note about a 1,500 sqft East plot and an installment plan.

## 4. Convert the lead and generate installments
1. Click **Convert** on Karan's row. Choose the new plot, ID proof type `Aadhaar Card`, a number, and the
   **Installment** payment plan.
2. Karan's lead becomes `CONVERTED`, and the plot is now `ON_HOLD` in Inventory.
3. Open **Installments**. There are six monthly tranches totalling 80% of ₹2,000,000, which is ₹1,600,000.
   Tranches 1 to 5 are ₹266,666.67 each, and the sixth is ₹266,666.65 so the total is exact. All are `PENDING`.

## 5. Payments and reconciliation
1. Open **Ledger** (the payments page) and record `400000` (the 20% down payment) for Karan as **Down Payment**.
2. In Inventory, the plot has moved from `ON_HOLD` to `BOOKED`.
3. Record `266666.67` as **Installment**. In **Installments**, tranche 1 is now `PAID` with the payment time, and the
   other five are still `PENDING`.

## 6. Sell a plot and see the commission
1. In Inventory, find Vikram Sharma's `BOOKED` plot (₹1,500,000) and move it to `SOLD`.
2. Open **Commissions**. A 2% commission of ₹30,000 was created for `agent`, and the leaderboard shows the agent's
   closed deals, sales volume and commission.

## 7. Dashboard
1. Open **Dashboard**. Review the stat cards, the status and revenue-trend charts, the lead pipeline, and the plot
   matrix. Switch the project filter and the 7 / 30 / 90 day range.
2. Click the executive summary button. The briefing covers portfolio health, collections, installment risk and a
   priority. It is produced by a rule-based engine, not an external AI service.

## 8. Audit log
1. Open **Audit Log** (admin only). You will see entries such as `SITE_TRANSITION`, `PAYMENT_RECORDED`,
   `LEAD_CREATED`, `LEAD_CONVERTED`, `SCHEDULE_GENERATED`, `COMMISSION_COMPUTED` and `PROJECT_CREATED`.
2. Each entry shows the action, the details, the user who performed it and the time.

## 9. REST API
```bash
curl -u admin:admin123 http://localhost:8080/api/v1/sites?status=AVAILABLE
curl -u admin:admin123 -X POST http://localhost:8080/api/v1/sites/1/transition \
     -H "Content-Type: application/json" -d '{"target":"ON_HOLD"}'
curl -u admin:admin123 http://localhost:8080/api/v1/commissions/leaderboard
```
An illegal transition, such as moving an `AVAILABLE` plot straight to `SOLD`, returns `409` with a JSON error.
