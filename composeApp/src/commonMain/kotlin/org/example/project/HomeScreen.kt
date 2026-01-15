package org.example.project

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import mistermanager.composeapp.generated.resources.Res
import mistermanager.composeapp.generated.resources.articles
import mistermanager.composeapp.generated.resources.help
import mistermanager.composeapp.generated.resources.home_button_articles
import mistermanager.composeapp.generated.resources.home_button_locations
import mistermanager.composeapp.generated.resources.home_button_statistics
import mistermanager.composeapp.generated.resources.home_title
import mistermanager.composeapp.generated.resources.info
import mistermanager.composeapp.generated.resources.info_how_to_help
import mistermanager.composeapp.generated.resources.info_screen_title
import mistermanager.composeapp.generated.resources.locations
import mistermanager.composeapp.generated.resources.logo
import mistermanager.composeapp.generated.resources.settings
import mistermanager.composeapp.generated.resources.settings_title
import mistermanager.composeapp.generated.resources.statistics
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenArticles: () -> Unit,
    onOpenLocations: () -> Unit,
    onOpenInfo: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHowToHelp: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = "MisterManager Logo",
                        modifier = Modifier.size(45.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(Res.string.home_title))
                }
            },
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeButton(onClick = onOpenArticles, icon = Res.drawable.articles, text = stringResource(Res.string.home_button_articles))
                HomeButton(onClick = onOpenLocations, icon = Res.drawable.locations, text = stringResource(Res.string.home_button_locations))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeButton(onClick = onOpenStatistics, icon = Res.drawable.statistics, text = stringResource(Res.string.home_button_statistics))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                HomeButton(onClick = onOpenSettings, icon = Res.drawable.settings, text = stringResource(Res.string.settings_title))
                HomeButton(onClick = onOpenInfo, icon = Res.drawable.info, text = stringResource(Res.string.info_screen_title))
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                HomeButton(onClick = onOpenHowToHelp, icon = Res.drawable.help, text = stringResource(Res.string.info_how_to_help))
            }
        }
    }
}

@Composable
private fun HomeButton(onClick: () -> Unit, icon: org.jetbrains.compose.resources.DrawableResource, text: String) {
    Button(
        onClick = onClick,
        shape = RectangleShape,
        modifier = Modifier.size(140.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Black)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(icon), contentDescription = text, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(8.dp))
            Text(text)
        }
    }
}
