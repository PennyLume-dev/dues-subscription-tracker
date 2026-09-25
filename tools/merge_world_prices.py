"""Merge the 12-country pricing dataset (subscriptions_pricing.json) into catalog.json.
Dry run by default; pass --write to save.

Rules:
- Services match by exact name, then unique domain, then SVC (dataset splits some of ours in two/three).
  Our display names are kept.
- Plans match by name + cycle: RENAME table, else the dataset name minus the service-name prefix,
  compared after norm(). Unmatched priced plans are added.
- $0 free plans and null amounts are skipped.
- Rows not in the country's own currency (Dropbox, Economist, Calm, Audible...) are USD-billed there:
  not stored as a local price, so the app shows a converted estimate instead.
- available:false -> country added to the plan's "x" list; the app hides the plan there.
"""
import json, re, sys
from datetime import date

CAT = str(__import__("pathlib").Path(__file__).resolve().parent.parent / "app/src/main/assets/catalog.json")
DATA = next((a for a in sys.argv[1:] if a.endswith(".json")), "subscriptions_pricing.json")  # dataset path as an argument
CUR = {"US": "USD", "IN": "INR", "GB": "GBP", "DE": "EUR", "FR": "EUR", "CA": "CAD",
       "AU": "AUD", "SG": "SGD", "AE": "AED", "BR": "BRL", "JP": "JPY", "MX": "MXN"}
# Dataset ids whose domain is shared by several of our services.
SVC = {"amazon-prime": "Amazon Prime", "amazon-prime-bundle": "Amazon Prime", "apple-tv-plus": "Apple TV",
       "microsoft-copilot": "Microsoft Copilot", "microsoft-365-personal-family": "Microsoft 365"}
# (our service, dataset plan) -> our plan name, matched after norm() and by cycle.
# None = skip the plan (vague, or priced under another of our services).
RENAME = {
    ("Amazon Prime", "Amazon Prime membership"): "Prime",
    ("Amazon Prime", "Amazon Prime Young Adults/student-eligible"): "Prime for Students and 18-22 year-olds",
    ("Amazon Prime", "Prime Student / Young Adults"): "Prime for Students and 18-22 year-olds",
    ("Disney+", "Disney+ standalone plan"): None,
    ("Max", "Max regular consumer tiers (not verified)"): None,
    ("Hulu", "Hulu with ads"): "Hulu (With Ads)",
    ("Hulu", "Hulu Premium"): "Hulu (No Ads)",
    ("Paramount+", "Basic with Ads"): "Essential",
    ("Sony LIV", "SonyLIV Premium"): "LIV Premium",
    ("ZEE5", "ZEE5 Premium"): "Premium - All Access",
    ("Spotify", "Premium Individual"): "Individual",
    ("Spotify", "Premium Student"): "Student",
    ("Spotify", "Premium Duo"): "Duo",
    ("Spotify", "Premium Family"): "Family",
    ("Spotify", "Premium Standard (India)"): "Standard",
    ("Spotify", "Premium Platinum (India)"): "Platinum",
    ("YouTube Music", "YouTube Music Premium"): "Individual",
    ("YouTube Music", "YouTube Music Premium Family"): "Family",
    ("Amazon Music Unlimited", "Individual (non-Prime)"): "Individual without Prime",
    ("Amazon Music Unlimited", "Individual (India non-Prime)"): "Individual without Prime",
    ("Amazon Music Unlimited", "Individual (Prime member)"): "Individual for Prime members",
    ("Amazon Music Unlimited", "Individual annual (Prime member)"): "Individual for Prime members",
    ("Amazon Music Unlimited", "Family annual (Prime member)"): "Family",
    ("Audible", "Premium"): "Premium / Premium Plus - 1 Credit",
    ("JioSaavn", "Pro Individual Monthly"): "Pro",
    ("JioSaavn", "Pro Individual Yearly"): "Pro",
    ("JioSaavn", "Pro Individual Monthly (App Store listing)"): None,
    ("ChatGPT", "ChatGPT Pro"): "Pro ($100)",
    ("Google One / Google AI plans", "Google AI Plus"): "Google AI Plus (400 GB)",
    ("Google One / Google AI plans", "Google AI Pro"): "Google AI Pro (5 TB)",
    ("Google One / Google AI plans", "Google AI Ultra"): "Google AI Ultra (5x)",
    ("Google One / Google AI plans", "Google AI Ultra 5x"): "Google AI Ultra (5x)",
    ("Google One / Google AI plans", "Google AI Ultra 20x"): "Google AI Ultra (20x)",
    ("Google One / Google AI plans", "Google One Basic"): "Basic (100 GB)",
    ("Google One / Google AI plans", "Google One Standard"): "Standard (200 GB)",
    ("Google One / Google AI plans", "Google One Lite (30 GB)"): "Lite (30 GB)",
    ("Google One / Google AI plans", "Google One Google AI Plus"): "Google AI Plus (400 GB)",
    ("Google One / Google AI plans", "Google One Google AI Pro"): "Google AI Pro (5 TB)",
    ("Google One / Google AI plans", "Google One Google AI Ultra 5x"): "Google AI Ultra (5x)",
    ("Google One / Google AI plans", "Google One Google AI Ultra 20x"): "Google AI Ultra (20x)",
    ("Microsoft Copilot", "Microsoft 365 Personal with Copilot"): None,
    ("Microsoft Copilot", "Microsoft 365 Family with Copilot"): None,
    ("Microsoft Copilot", "Microsoft 365 Premium"): None,
    ("OneDrive", "Microsoft 365 Basic"): "100 GB",
    ("OneDrive", "Microsoft 365 Personal"): None,
    ("OneDrive", "Microsoft 365 Family"): None,
    ("OneDrive", "Microsoft 365 Premium (monthly)"): None,
    ("OneDrive", "Microsoft 365 Premium (annual)"): None,
    ("Meta Verified", "Meta Verified creator — one profile"): "Meta Verified",
    ("Snapchat+", "Family Plan"): "Family",
    ("Telegram Premium", "Telegram Premium"): "Premium",
    # Dataset prices Business Standard, and its yearly $14 is a per-month rate, not an annual total.
    ("Google Workspace Individual", "Workspace Individual"): None,
    ("Adobe Creative Cloud", "Creative Cloud Pro (All Apps)"): "Creative Cloud Pro",
    ("Zoom", "Pro"): "Workplace Pro",
    ("Nintendo Switch Online", "Nintendo Switch Online Individual"): "Individual 12 Month",
    ("Nintendo Switch Online", "Nintendo Switch Online Family"): "Family 12 Month",
    ("Nintendo Switch Online", "Nintendo Switch Online Individual (1 month)"): "Individual 1 Month",
    ("Nintendo Switch Online", "Nintendo Switch Online Individual (3 months)"): "Individual 3 Month",
    ("EA Play", "EA Play Pro (monthly)"): "Pro",
    ("EA Play", "EA Play Pro (annual)"): "Pro",
    ("Coursera Plus", "Coursera Plus individual monthly"): "Plus",
    ("Coursera Plus", "Coursera Plus individual yearly"): "Plus",
    ("MasterClass", "MasterClass membership plan 1 (annual)"): "Membership",
    ("Strava", "Strava individual monthly"): "Individual",
    ("Strava", "Strava individual yearly"): "Individual",
    ("The New York Times", "All Access (1 person)"): "All Access",
    ("The Economist", "Premium digital"): "Digital",
    ("Medium", "Medium Member"): "Membership",
    ("Apple One", "Apple One Premier"): "Premier / Premium",
}
CYCLE_WORDS = {"annual", "yearly", "monthly", "prepaid"}
# Prices we deliberately keep over the dataset: (service, plan, u, k, country).
KEEP = {
    ("Grammarly", "Pro", "month", 1, "US"),  # dataset $12 is the annual plan's monthly equivalent
    ("YouTube Music", "Individual", "month", 1, "IN"),  # dataset 155 is the iOS App Store price; Play/web is 119
    # Checked by hand 2026-09-25 against official pages; dataset was wrong.
    ("JioHotstar", "Premium", "year", 1, "IN"),  # 2199 (plans effective 28 Jan 2026)
    ("ZEE5", "Premium - All Access", "month", 1, "IN"),
    ("ZEE5", "Premium - All Access", "month", 3, "IN"),
    ("ZEE5", "Premium - All Access", "year", 1, "IN"),
    ("ChatGPT", "Plus", "month", 1, "US"),
    ("Reddit", "Premium", "month", 1, "US"),
    ("Reddit", "Premium Annual", "year", 1, "US"),
    ("Deezer", "Premium", "month", 1, "US"),
    ("Deezer", "Family", "month", 1, "US"),
    ("PlayStation Plus", "Premium", "month", 3, "US"),  # unverified either way; ours kept
}
# Hand-verified prices that neither we nor the dataset had right.
OVERRIDE = {
    ("PlayStation Plus", "Essential", "month", 1, "US"): 9.99,
    ("PlayStation Plus", "Extra", "month", 1, "US"): 14.99,
    ("PlayStation Plus", "Premium", "month", 1, "US"): 17.99,
    ("Deezer", "Premium Annual", "year", 1, "US"): 107.91,
    ("Deezer", "Duo", "month", 1, "US"): 15.99,
    ("Deezer", "Duo Annual", "year", 1, "US"): 174.99,
    ("Deezer", "Student", "month", 1, "US"): 5.99,
    ("YouTube Music", "Family", "month", 1, "IN"): 179,
    ("YouTube Music", "Student", "month", 1, "IN"): 59,
}
# AI tools bill the web in USD everywhere; the dataset's local prices for them are iOS App Store SKUs
# (Apple markup), so those rows are skipped and the app shows a converted US estimate.
USD_WEB = {"chatgpt", "claude", "perplexity", "grok", "midjourney"}
# Services not sold in a country (dataset left these null instead of available:false).
ALL = ["US", "IN", "GB", "DE", "FR", "CA", "AU", "SG", "AE", "BR", "JP", "MX"]
NOT_SOLD = {"Hulu": [c for c in ALL if c != "US"], "Peacock": [c for c in ALL if c != "US"],
            "Disney+": ["IN"], "Max": ["IN"]}  # India gets Disney+ and HBO via JioHotstar
# Dataset rows that are wrong: (dataset service id, plan, cycle, country).
BAD = {("dropbox", "Dropbox Plus", "year", "US")}  # $9.99 is the per-month rate of the yearly plan


def num(x):
    return int(x) if float(x).is_integer() else x


def find_service(cat, s):
    by_name = {c["n"]: c for c in cat["services"]}
    if s["id"] in SVC:
        return by_name[SVC[s["id"]]]
    if s["name"] in by_name:
        return by_name[s["name"]]
    same = [c for c in cat["services"] if c["d"] == s["domain"]]
    return same[0] if len(same) == 1 else None


def strip_prefix(name, *prefixes):
    """"Reddit Premium" minus "Reddit" -> "Premium"; a name that is only the service name -> ""."""
    for pre in prefixes:
        if name.lower().startswith(pre.lower() + " "):
            return name[len(pre) + 1:]
    return "" if any(name.lower() == pre.lower() for pre in prefixes) else name


def norm(name):
    """Plan name as comparable tokens: "Premium - 12 Credits" == "Premium 12 credits", "50 GB" == "50GB".
    Cycle words are dropped since the cycle is compared separately ("Premium Annual"/year == "Premium"/year)."""
    s = re.sub(r"(\d)\s+(gb|tb)", r"\1\2", name.lower())
    return " ".join(t for t in re.findall(r"[a-z0-9+]+", s) if t != "+" and t not in CYCLE_WORDS)


def target_name(svc, dsvc, p):
    """Our name for a dataset plan, or None to skip it."""
    key = (svc["n"], p["name"])
    if key in RENAME:
        return RENAME[key]
    return strip_prefix(p["name"], dsvc["name"], svc["n"])


def find_plan(svc, name, p):
    want = norm(name)
    for q in svc["p"]:
        if q["u"] == p["cycle"] and q["k"] == p["cycle_count"] and norm(q["n"]) == want:
            return q
    return None


def merge(cat, data, log=print):
    stats = dict(same=0, updated=0, new_price=0, usd_billed=0, free=0, kept=0, hidden=0, conflict=0, added_plans=0)
    # Several dataset plans can map to one of ours (the dataset splits Amazon Prime and Google in 2-3),
    # so availability and prices are settled per (our plan, country) across all of them.
    seen = {}         # (id(plan), country) -> price set this run
    offered = set()   # (id(plan), country) some source says is sold
    unavailable = []  # (service name, plan, country) some source says isn't sold
    for ds in data["services"]:
        svc = find_service(cat, ds)
        if svc is None:
            log(f"NEW SERVICE  {ds['name']} ({ds['domain']})")
            svc = {"n": ds["name"], "d": ds["domain"], "c": ds["category"], "p": []}
            cat["services"].append(svc)
        for p in ds["plans"]:
            rows = p["prices"]
            priced = {r["country"]: r for r in rows if r["available"] and r["amount"] is not None}
            if priced and all(r["amount"] == 0 for r in priced.values()):
                stats["free"] += 1
                continue
            name = target_name(svc, ds, p)
            if name is None:
                continue
            plan = find_plan(svc, name, p)
            if plan is None:
                if not any(r["amount"] for r in priced.values() if r["currency"] == CUR[r["country"]]):
                    continue  # nothing usable to add
                name = name or p["name"]
                if p["cycle"] == "year" and "annual" not in name.lower():
                    name += " Annual"
                plan = {"n": name, "u": p["cycle"], "k": p["cycle_count"], "fam": p["is_family"],
                        "stu": p["is_student"], "t": p["free_trial_days"] or 0, "pr": {}}
                svc["p"].append(plan)
                stats["added_plans"] += 1
                log(f"ADD PLAN     {svc['n']:28} {name} /{p['cycle']}{p['cycle_count']}")
            for r in rows:
                c = r["country"]
                if not r["available"]:
                    unavailable.append((svc["n"], plan, c))
                    continue
                offered.add((id(plan), c))
                amt = r["amount"]
                if amt is None or amt == 0 or (ds["id"], p["name"], p["cycle"], c) in BAD:
                    continue
                if r["currency"] != CUR[c] or (ds["id"] in USD_WEB and c != "US" and "apps.apple.com" in (r["source_url"] or "")):
                    stats["usd_billed"] += 1
                    continue
                if (svc["n"], plan["n"], plan["u"], plan["k"], c) in KEEP and c in plan["pr"]:
                    stats["kept"] += 1
                    log(f"KEEP         {svc['n']:28} {plan['n']:30} {c} {plan['pr'][c]} (dataset {amt})")
                    continue
                if (id(plan), c) in seen:
                    if seen[id(plan), c] != amt:
                        log(f"DISAGREE     {svc['n']:28} {plan['n']:30} {c} kept {seen[id(plan), c]}, other source {num(amt)}")
                    continue
                seen[id(plan), c] = amt
                old = plan["pr"].get(c)
                if old == amt:
                    stats["same"] += 1
                elif old is None:
                    stats["new_price"] += 1
                else:
                    stats["updated"] += 1
                    log(f"UPDATE       {svc['n']:28} {plan['n']:30} {c} {old} -> {num(amt)}")
                plan["pr"][c] = num(amt)
    for sname, plan, c in unavailable:
        if (id(plan), c) in offered or c in plan.get("x", []):
            continue
        if c in plan["pr"]:  # a price we verified earlier; existing subscribers may still pay it
            stats["conflict"] += 1
            log(f"CONFLICT     {sname:28} {plan['n']:30} {c} has {plan['pr'][c]} but dataset says not sold; kept price")
            continue
        plan.setdefault("x", []).append(c)
        plan["x"].sort()
        stats["hidden"] += 1
    for svc in cat["services"]:
        # Old catalog repeated plans (Apple Music x4): fold same name+cycle into one.
        merged = {}
        for p in svc["p"]:
            key = (p["n"], p["u"], p["k"])
            if key in merged:
                merged[key]["pr"] = {**p["pr"], **merged[key]["pr"]}
                merged[key]["x"] = sorted(set(merged[key].get("x", [])) & set(p.get("x", [])))
                if not merged[key]["x"]:
                    del merged[key]["x"]
            else:
                merged[key] = p
        svc["p"] = list(merged.values())
        for p in svc["p"]:
            extra = [c for c in NOT_SOLD.get(svc["n"], []) if c not in p["pr"]]
            if extra:
                p["x"] = sorted(set(p.get("x", [])) | set(extra))
    for (sname, pname, u, k, c), price in OVERRIDE.items():
        svc = next(s for s in cat["services"] if s["n"] == sname)
        plan = next((p for p in svc["p"] if p["n"] == pname and p["u"] == u and p["k"] == k), None)
        if plan is None:
            plan = {"n": pname, "u": u, "k": k, "fam": False, "stu": "Student" in pname, "t": 0, "pr": {}}
            svc["p"].append(plan)
        log(f"OVERRIDE     {sname:28} {pname:30} {c} {plan['pr'].get(c)} -> {price}")
        plan["pr"][c] = price
    return stats


if __name__ == "__main__":
    cat = json.load(open(CAT, encoding="utf-8"))
    stats = merge(cat, json.load(open(DATA, encoding="utf-8")))
    print("\n" + "  ".join(f"{k}={v}" for k, v in stats.items()))
    if "--write" in sys.argv:
        cat["v"] = date.today().isoformat()  # app downloads a catalog only if its "v" is newer
        json.dump(cat, open(CAT, "w", encoding="utf-8"), ensure_ascii=False, separators=(",", ":"))
        print("written")
