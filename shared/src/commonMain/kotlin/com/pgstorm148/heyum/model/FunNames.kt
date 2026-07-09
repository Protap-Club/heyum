package com.pgstorm148.heyum.model

/**
 * A pool of playful display names assigned to people in a group session so members are
 * distinguishable without anyone having to type their real name. Names are picked randomly
 * and uniquely per session (see [pickUnique]).
 */
object FunNames {
    private val names: List<String> = listOf(
        // Marvel Avengers
        "Iron Man", "Captain America", "Thor", "Black Widow", "Hulk", "Hawkeye",
        "Spider-Man", "Doctor Strange", "Black Panther", "Scarlet Witch", "Vision",
        "Ant-Man", "The Wasp", "Falcon", "Winter Soldier", "Star-Lord", "Gamora",
        "Groot", "Rocket", "Nebula", "Loki", "Nick Fury", "Wolverine", "Deadpool",
        // Suits characters
        "Harvey Specter", "Mike Ross", "Louis Litt", "Donna Paulsen", "Rachel Zane",
        "Jessica Pearson", "Katrina Bennett", "Robert Zane", "Harold Gunderson",
        // Kids comics & cartoons
        "Tintin", "Asterix", "Obelix", "Calvin", "Hobbes", "Garfield", "Snoopy",
        "Popeye", "Dennis", "Archie", "Jughead", "Chacha Chaudhary", "Suppandi",
        // Fun-loving good names
        "Maverick", "Ace", "Scout", "Sunny", "Biscuit", "Pixel", "Ziggy", "Nova",
        "Rebel", "Blaze", "Comet", "Dash", "Echo", "Bolt", "Waffles", "Peanut",
        "Mango", "Noodle", "Pickle", "Cosmo", "Juno", "Ranger"
    )

    /**
     * Returns a random name not already used by anyone in [taken]. If the whole pool is
     * somehow exhausted (very large session), falls back to appending a number so the result
     * is still unique.
     */
    fun pickUnique(taken: Collection<String>): String {
        val takenSet = taken.toSet()
        val available = names.filter { it !in takenSet }
        if (available.isNotEmpty()) return available.random()

        val base = names.random()
        var suffix = 2
        var candidate = "$base $suffix"
        while (candidate in takenSet) {
            suffix++
            candidate = "$base $suffix"
        }
        return candidate
    }
}
