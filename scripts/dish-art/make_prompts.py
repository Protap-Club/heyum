#!/usr/bin/env python3
"""Builds docs/dish-prompts.csv: one ready-to-paste image prompt per dish."""
import csv, os, re, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
POOL = os.environ.get("DISHES_POOL", "/Users/family/AndroidStudioProjects/heyum/shared/src/commonMain/kotlin/com/pgstorm148/heyum/model/DishesPool.kt")
OUT = os.path.join(ROOT, "docs", "dish-prompts.csv")

CUISINE = {"INDIAN": "Indian", "CHINESE": "Chinese", "ITALIAN": "Italian", "MEXICAN": "Mexican", "THAI": "Thai",
           "JAPANESE": "Japanese", "MEDITERRANEAN": "Mediterranean", "HEALTHY": "healthy", "COMFORT": "comfort-food",
           "FAST_FOOD": "fast-food", "STREET_FOOD": "Indian street food"}

# first keyword match wins -> (how it is served, backdrop)
RULES = [
    (("smoothie bowl",), "in a wide white ceramic bowl topped with sliced banana, berries, seeds and granola in neat rows", "light marble table"),
    (("sushi", "onigiri"), "on a dark wooden serving board with soy sauce, wasabi and pickled ginger", "dark slate table"),
    (("spring roll",), "on a white plate, golden and crisp, one cut diagonally to show the filling, with a sweet chilli dipping sauce", "dark slate table"),
    (("shawarma plate",), "on a large white plate with rice, sliced shawarma meat, salad, garlic sauce and pita", "wooden table"),
    (("katsu curry",), "on a white plate with rice, sliced golden cutlet and a pool of Japanese curry sauce", "wooden table"),
    (("shakshuka",), "in a cast-iron skillet with eggs poached in spiced tomato sauce", "dark slate table"),
    (("bruschetta", "garlic bread"), "on a wooden board", "rustic wooden table"),
    (("hummus", "caprese"), "on a large round white platter, arranged neatly", "light marble or wooden table"),
    (("pancake", "waffle"), "stacked on a white plate with syrup, butter and berries", "wooden table"),
    (("sticky rice",), "on a white plate with sliced ripe mango and coconut cream", "light linen table"),
    (("omelette", "nasi goreng"), "on a white plate", "wooden table"),
    (("smoothie", "latte", "tea", "chai"), "in a glass or ceramic mug, with a small garnish beside it", "light marble or wooden table"),
    (("mug cake",), "in a white ceramic mug on a small saucer", "dark slate table"),
    (("soup", "pho", "ramen", "thukpa", "minestrone", "rasam"), "in a deep ceramic bowl with a spoon resting beside it", "dark slate or wooden table"),
    (("pizza",), "on a round wooden pizza board, sliced", "rustic wooden table"),
    (("burger",), "on a white plate, one burger whole and one open to show the layers, with a side of fries", "wooden table"),
    (("sandwich", "toast"), "on a wooden board, cut in half to show the layers", "light wooden table"),
    (("wrap", "roll", "frankie", "gyro", "shawarma"), "on a wooden board, one cut in half to show the filling, partly wrapped in paper", "rustic wooden table"),
    (("taco", "quesadilla", "tostada", "nachos", "enchilada", "fajita", "burrito"), "on a dark ceramic plate or skillet with small dip bowls beside it", "terracotta-toned rustic table"),
    (("dumpling", "gyoza", "momo"), "in a bamboo steamer or on a dark plate, with a small dipping sauce bowl", "dark slate table"),
    (("salad", "bowl", "bibimbap", "couscous", "quinoa"), "in a wide white ceramic bowl, ingredients arranged colourfully", "light marble or sage-green table"),
    (("noodle", "pad thai", "carbonara", "penne", "lasagna", "risotto", "mac and cheese"), "on a white plate or shallow bowl", "wooden or linen table"),
    (("biryani", "pulao", "rice", "khichdi", "poha"), "in a bowl or shallow serving dish with garnish on top", "dark slate or linen table"),
    (("dosa", "medu vada", "idli", "paratha", "pesarattu", "appam", "bhature", "litti"), "on a steel or white plate with small bowls of chutney and sambar or curry beside it", "wooden table"),
    (("puri", "chaat", "kachori", "bread pakora", "dabeli", "vada pav", "misal", "pav", "onion rings", "fries", "fish & chips", "bhel"), "street-style on a plate or paper tray", "rustic wooden table"),
    (("curry", "masala", "kofta", "dal", "rajma", "chole", "bhurji", "kadhi", "stew", "chilli", "maas", "rogan", "chettinad", "aloo", "gobhi", "baingan", "bharta", "korma", "paneer", "manchurian", "undhiyu", "prawn", "butter chicken", "chicken curry", "egg curry"), "in a copper or ceramic serving bowl, garnished with fresh coriander", "dark slate table"),
    (("tikka", "tandoori", "satay", "souvlaki", "kebab", "teriyaki", "katsu", "fish with"), "plated neatly on a plate with simple sides", "dark slate or wooden table"),
]


def serving(name):
    n = name.lower()
    for keys, how, bg in RULES:
        if any(re.search(r"\b" + re.escape(k), n) for k in keys):
            return how, bg
    return "plated attractively in a suitable ceramic dish", "dark slate or wooden table"


STYLE = ("Professional overhead (top-down) food photograph, natural soft window light from the upper left, shallow depth of field, "
         "rich realistic textures, glossy sauce or gentle steam where appropriate, fresh garnish. {bg} with a subtle vignette. "
         "One or two small props at the edges (a spice bowl, lime wedge or herbs) that never cover the dish. "
         "The dish is centred and fills about 60% of the frame with clear margin on every side. "
         "4:3 landscape, 1600x1200, photorealistic and appetising. No text, no watermark, no logos, no hands, no people.")


def main():
    t = open(POOL).read()
    rows = re.findall(r'Dish\(\s*id = "([^"]+)",\s*name = "([^"]+)",\s*cuisine = Cuisine\.(\w+),\s*description = "([^"]*)".*?ingredients = listOf\(([^)]*)\)', t, re.S)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["dish_id", "save_as", "name", "cuisine", "prompt"])
        for id_, name, cuisine, desc, ing in rows:
            ings = ", ".join(re.findall(r'"([^"]+)"', ing))
            how, bg = serving(name)
            prompt = (f"{name} ({CUISINE.get(cuisine, cuisine.title())} cuisine), served {how}. "
                      f"{desc.rstrip('.')}. Key ingredients visible: {ings}. " + STYLE.format(bg=bg[0].upper() + bg[1:]))
            w.writerow([id_, f"{id_}.jpg", name, CUISINE.get(cuisine, cuisine.title()), prompt])
    print(f"{len(rows)} prompts -> {OUT}")


if __name__ == "__main__":
    main()
