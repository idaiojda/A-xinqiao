package com.example.xinqiao.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunitySearchBar(
    searchText: String,
    onSearch: (String) -> Unit,
    categories: List<String>,
    selectedCategory: Int,
    onSelectCategory: (Int) -> Unit
) {
    val tokens = CommunityTokensInstance
    Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.L)) {
        // Search Field
        TextField(
            value = searchText,
            onValueChange = onSearch,
            placeholder = { 
                Text(
                    "搜索话题、用户…", 
                    color = tokens.color.Neutral700,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 4.dp),
                    style = androidx.compose.ui.text.TextStyle(
                        lineHeight = 16.sp
                    )
                ) 
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = tokens.color.Neutral500,
                    modifier = Modifier.size(18.dp)
                )
            },
            shape = RoundedCornerShape(tokens.corner.Card),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = tokens.color.Surface,
                unfocusedContainerColor = tokens.color.Surface,
                focusedIndicatorColor = tokens.color.Primary,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 13.sp,
                color = tokens.color.Neutral700,
                lineHeight = 16.sp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        )

        // Category Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.S)) {
            itemsIndexed(categories) { index, name ->
                FilterChip(
                    selected = index == selectedCategory,
                    onClick = { onSelectCategory(index) },
                    label = {
                        Text(
                            name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    shape = RoundedCornerShape(tokens.corner.Chip)
                )
            }
        }
    }
}