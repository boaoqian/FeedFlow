package com.qba.feedflow.data

val testData = listOf<RssItem>(
    RssItem(
        id = 1L,
        channel = "Tech News",
        title = "New AI Breakthrough in Neural Networks",
        link = "https://technews.com/article1",
        description = "Scientists announce a major advancement in AI technology.",
        pubDate = 1625097600000L, // June 1, 2025
        read = false
    ),
    RssItem(
        id = 2L,
        channel = "Science Daily",
        title = "Quantum Computing Milestone Achieved",
        link = "https://sciencedaily.com/article2",
        description = "Researchers demonstrate quantum supremacy in latest experiment.",
        pubDate = 1625184000000L, // June 2, 2025
        read = true
    ),
    RssItem(
        id = 3L,
        channel = "Tech Crunch",
        title = "Startup Unveils Revolutionary Battery Tech",
        link = "https://techcrunch.com/article3",
        description = "New battery promises 10x longer lifespan for devices.",
        pubDate = 1625270400000L, // June 3, 2025
        read = false
    ),
    RssItem(
        id = 4L,
        channel = "Wired",
        title = "AR Glasses Set to Launch Next Year",
        link = "https://wired.com/article4",
        description = "Augmented reality glasses to redefine user experience.",
        pubDate = 1625356800000L, // June 4, 2025
        read = false
    ),
    RssItem(
        id = 5L,
        channel = "The Verge",
        title = "Self-Driving Cars Hit Mainstream Roads",
        link = "https://theverge.com/article5",
        description = "Autonomous vehicles now available for public use.",
        pubDate = 1625443200000L, // June 5, 2025
        read = true
    ),
    RssItem(
        id = 6L,
        channel = "Gizmodo",
        title = "Space Tourism Prices Slashed",
        link = "https://gizmodo.com/article6",
        description = "Affordable space travel becomes reality for many.",
        pubDate = 1625529600000L, // June 6, 2025
        read = false
    ),
    RssItem(
        id = 7L,
        channel = "Ars Technica",
        title = "New Encryption Standard Released",
        link = "https://arstechnica.com/article7",
        description = "Enhanced security protocols for global communications.",
        pubDate = 1625616000000L, // June 7, 2025
        read = false
    ),
    RssItem(
        id = 8L,
        channel = "CNET",
        title = "Holographic Displays Now in Stores",
        link = "https://cnet.com/article8",
        description = "Holography technology hits consumer markets.",
        pubDate = 1625702400000L, // June 8, 2025
        read = true
    ),
    RssItem(
        id = 9L,
        channel = "Engadget",
        title = "Wearable Health Tech Gets Smarter",
        link = "https://engadget.com/article9",
        description = "New wearables monitor health with unprecedented accuracy.",
        pubDate = 1625788800000L, // June 9, 2025
        read = false
    ),
    RssItem(
        id = 10L,
        channel = "Mashable",
        title = "VR Gaming Reaches New Heights",
        link = "https://mashable.com/article10",
        description = "Virtual reality gaming experiences redefined.",
        pubDate = 1625875200000L, // June 10, 2025
        read = false
    )
)

val testchannel = listOf<String>(
    "Tech News",
    "Science Daily",
    "Tech Crunch",
    "Wired",
    "The Verge")