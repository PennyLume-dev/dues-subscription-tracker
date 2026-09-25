"""Copies the site to dist/, filling in the support email.
Usage: python build.py [support@email]  (no email -> "support inbox coming soon")."""
import shutil, sys, pathlib
root = pathlib.Path(__file__).parent
dist = root / "dist"
# Keep dist/.vercel (the Vercel project link); replace everything else.
dist.mkdir(exist_ok=True)
for item in dist.iterdir():
    if item.name != ".vercel":
        shutil.rmtree(item) if item.is_dir() else item.unlink()
shutil.copytree(root, dist, ignore=shutil.ignore_patterns("dist", "build.py", ".vercel"), dirs_exist_ok=True)
email = sys.argv[1] if len(sys.argv) > 1 else None
for f in dist.glob("*.html"):
    s = f.read_text(encoding="utf-8")
    link = '<a href="mailto:SUPPORT_EMAIL">SUPPORT_EMAIL</a>'
    s = s.replace(link, f'<a href="mailto:{email}">{email}</a>' if email else "our support inbox (launching soon)")
    f.write_text(s, encoding="utf-8")
# The app downloads this price list at launch (see Catalog.kt CATALOG_URL); the app's asset is the source of truth.
shutil.copy(root.parent / "app" / "src" / "main" / "assets" / "catalog.json", dist / "catalog.json")
print("built", dist, "email:", email or "(coming soon)")
