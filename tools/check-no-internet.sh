#!/usr/bin/env bash
# Verifică regula de aur din SPEC §9: aplicația nu are voie să ceară rețea.
#
# Folosire:
#   ./gradlew :app:assembleDebug && bash tools/check-no-internet.sh
#
# Iese cu cod 1 dacă a apărut o permisiune de rețea, fie scrisă de noi în manifest,
# fie adusă de o bibliotecă și fuzionată în manifestul final.
# Caută doar declarații reale, adică atributul android:name, nu mențiuni în comentarii
# sau markere explicite tools:node="remove" folosite pentru manifest merger.

set -uo pipefail

FORBIDDEN=(
	"android.permission.INTERNET"
	"android.permission.ACCESS_NETWORK_STATE"
	"android.permission.ACCESS_WIFI_STATE"
)

fail=0

check_file() {
	local file="$1"
	local perm
	local matches
	for perm in "${FORBIDDEN[@]}"; do
		# Acceptă spații în jurul semnului egal și oricare tip de ghilimele.
		# O linie cu tools:node="remove" elimină permisiunea din manifestul final,
		# deci nu este o cerere de permisiune și nu trebuie raportată ca încălcare.
		matches="$(grep -E "android:name[[:space:]]*=[[:space:]]*[\"']$perm[\"']" "$file" 2>/dev/null || true)"
		if [ -n "$matches" ] && printf '%s\n' "$matches" | grep -Evq "tools:node[[:space:]]*=[[:space:]]*[\"']remove[\"']"; then
			echo "EȘEC: $perm apare în $file"
			fail=1
		fi
	done
}

# 1. Manifestele scrise de noi
while IFS= read -r -d '' manifest; do
	check_file "$manifest"
done < <(find app/src -name "AndroidManifest.xml" -print0 2>/dev/null)

# 2. Manifestele fuzionate (prind și ce aduc bibliotecile)
merged_count=0
while IFS= read -r -d '' manifest; do
	merged_count=$((merged_count + 1))
	check_file "$manifest"
done < <(find app/build/intermediates -name "AndroidManifest.xml" -print0 2>/dev/null)

if [ "$merged_count" -eq 0 ]; then
	echo "ATENȚIE: nu am găsit manifeste fuzionate. Rulează întâi: ./gradlew :app:assembleDebug"
fi

if [ "$fail" -ne 0 ]; then
	echo
	echo "Regula de aur încălcată: aplicația trebuie să funcționeze 100% offline."
	echo "Scoate permisiunea sau renunță la funcția care are nevoie de ea."
	exit 1
fi

echo "OK: nicio permisiune de rețea ($merged_count manifest(e) fuzionat(e) verificat(e))."
