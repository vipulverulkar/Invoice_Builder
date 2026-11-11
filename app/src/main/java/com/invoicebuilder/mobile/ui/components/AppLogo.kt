package com.invoicebuilder.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.invoicebuilder.mobile.R
import com.invoicebuilder.mobile.ui.theme.InvoiceBuilderTheme

/**
 * Modern 3D Vector App Logo for Invoice Builder.
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
) {
    Image(
        painter = painterResource(id = R.drawable.ic_app_logo),
        contentDescription = "Invoice Builder 3D Logo",
        modifier = modifier.size(size),
    )
}

/**
 * App Logo Header Card with 3D Logo and Brand Typography.
 */
@Composable
fun AppLogoBrandingCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                AppLogo(size = 64.dp)
            }

            Column {
                Text(
                    text = "Invoice Builder",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Fast, Compliant & Professional Invoicing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AppLogoPreview() {
    InvoiceBuilderTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            AppLogo(size = 140.dp)
            AppLogoBrandingCard()
        }
    }
}
