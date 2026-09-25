"""Merge verified India INR prices from the PremiumKing CSV into catalog.json.
USD-only rows (AI/dev tools billed in USD in India) are skipped: those services charge
Indian cards in USD, so the app's live USD->INR conversion is the right behaviour."""
import csv, json, re, sys
CAT = str(__import__("pathlib").Path(__file__).resolve().parent.parent / "app/src/main/assets/catalog.json")
CSV = next((a for a in sys.argv[1:] if a.endswith(".csv")), "premiumking-subscription-price-index-india-2026-08.csv")  # CSV path as an argument
WRITE = "--write" in sys.argv
SVC = {"Apple iCloud+": "iCloud+", "SonyLIV": "Sony LIV", "Google": "Google One / Google AI plans",
       "Amazon Music": "Amazon Music Unlimited", "Amazon Prime Video": "Amazon Prime"}
CYCLE = {"monthly": ("month", 1), "quarterly": ("month", 3), "annual": ("year", 1)}
STOP = {"premium", "plan", "membership", "full", "annual", "monthly", "individual", "music", "option", "usage", "the", "-", "for", "members", "member"}

# Explicit renames where the CSV and catalog name the same plan differently: (service, csv plan, billing) -> catalog plan.
RENAME = {
    ("Spotify", "Premium Standard", "annual"): "Standard Annual Prepaid",
    ("Amazon Prime", "Prime (full membership)", "monthly"): "Prime Monthly",
    ("Amazon Prime", "Prime (full membership)", "annual"): "Prime Annual",
    ("ZEE5", "Tamil / Telugu / Malayalam / Marathi pack", "monthly"): "Tamil",
    ("ZEE5", "Tamil / Telugu / Malayalam / Marathi pack", "annual"): "Tamil",
    ("ZEE5", "Kidz pack", "monthly"): "Kidz",
    ("Amazon Music Unlimited", "Unlimited (Prime member)", "monthly"): "Individual for Prime members",
    ("Amazon Music Unlimited", "Unlimited (non-Prime)", "monthly"): "Individual without Prime",
    ("YouTube Music", "Music Premium Individual", "monthly"): "Individual",
    ("Google One / Google AI plans", "AI Plus", "monthly"): "Google AI Plus (400 GB)",
    ("Google One / Google AI plans", "AI Pro", "monthly"): "Google AI Pro (5 TB)",
    ("Google One / Google AI plans", "AI Ultra - 5x usage option", "monthly"): "Google AI Ultra (5x)",
    ("Google One / Google AI plans", "AI Ultra - 20x usage option", "monthly"): "Google AI Ultra (20x)",
}

def toks(s):
    s = re.sub(r"(\d)\s+(gb|tb)", r"\1\2", s.lower())  # "50 GB" == "50GB"
    return frozenset(t for t in re.findall(r"[a-z0-9+]+", s) if t not in STOP)

cat = json.load(open(CAT, encoding="utf-8"))
by = {s["n"]: s for s in cat["services"]}
rows = [r for r in csv.DictReader(open(CSV, encoding="utf-8-sig")) if r["price_inr"].strip()]
added = updated = same = 0
for r in rows:
    s = by.get(SVC.get(r["service"], r["service"]))
    if not s:
        print("NO SERVICE", r["service"]); continue
    u, k = CYCLE[r["billing"]]
    price = float(r["price_inr"]); price = int(price) if price.is_integer() else price
    want = RENAME.get((s["n"], r["plan"], r["billing"]))
    rt = toks(r["plan"])
    cands = [p for p in s["p"] if p["u"] == u and p["k"] == k and (p["n"] == want if want else toks(p["n"]) == rt)]
    # Prefer a plan already carrying an IN price, then one with no IN price.
    cands.sort(key=lambda p: ("IN" not in p["pr"], len(p["pr"])))
    if cands:
        p = cands[0]; old = p["pr"].get("IN")
        tag = "same" if old == price else f"{old} -> {price}"
        if old == price: same += 1
        else: updated += 1
        print(f"MATCH  {s['n']:28} {r['plan'][:28]:28} {r['billing']:9} => '{p['n']}' IN {tag}")
        p["pr"]["IN"] = price
    else:
        added += 1
        name = r["plan"] + (" Annual" if u == "year" and "annual" not in r["plan"].lower() else "")
        print(f"ADD    {s['n']:28} {name[:40]:40} {r['billing']:9} IN {price}")
        s["p"].append({"n": name, "u": u, "k": k, "fam": "family" in r["plan"].lower(), "stu": "student" in r["plan"].lower(), "t": 0, "pr": {"IN": price}})
print(f"\nrows={len(rows)} same={same} updated={updated} added={added}")
if WRITE:
    cat["v"] = __import__("datetime").date.today().isoformat()  # app downloads a catalog only if its "v" is newer
    json.dump(cat, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
    print("written")
