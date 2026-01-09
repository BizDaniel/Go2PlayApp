package com.example.go2play.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.go2play.data.model.Notification
import com.example.go2play.data.model.NotificationStatus
import com.example.go2play.data.model.NotificationType
import org.threeten.bp.LocalDateTime
import androidx.compose.foundation.layout.padding
import org.threeten.bp.Duration
import org.threeten.bp.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.notificationState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Mostra errori
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Notifications", fontWeight = FontWeight.Bold)
                },
                windowInsets = WindowInsets(0),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.pendingCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Text(state.pendingCount.toString())
                        }
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.notifications.isEmpty() -> {
                    EmptyNotificationsView(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Raggruppa per pending e altre
                        val pendingNotifications = state.notifications.filter {
                            it.status == NotificationStatus.PENDING
                        }
                        val otherNotifications = state.notifications.filter {
                            it.status != NotificationStatus.PENDING
                        }

                        if (pendingNotifications.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Pending Invites",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(pendingNotifications) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onAccept = {
                                        viewModel.acceptInvite(notification)
                                    },
                                    onDecline = {
                                        viewModel.declineInvite(notification.id)
                                    },
                                    onDelete = {
                                        viewModel.deleteNotification(notification.id)
                                    },
                                    isAccepting = state.isAccepting,
                                    isDeclining = state.isDeclining
                                )
                            }
                        }

                        if (otherNotifications.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Past Notifications",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(
                                        PaddingValues(
                                            top = 16.dp,
                                            bottom = 8.dp
                                        )
                                    )
                                )
                            }

                            items(otherNotifications) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onAccept = null,
                                    onDecline = null,
                                    onDelete = {
                                        viewModel.deleteNotification(notification.id)
                                    },
                                    isAccepting = false,
                                    isDeclining = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: Notification,
    onAccept: (() -> Unit)?,
    onDecline: (() -> Unit)?,
    onDelete: () -> Unit,
    isAccepting: Boolean,
    isDeclining: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (notification.status) {
                NotificationStatus.PENDING -> MaterialTheme.colorScheme.primaryContainer
                NotificationStatus.ACCEPTED -> MaterialTheme.colorScheme.secondaryContainer
                NotificationStatus.DECLINED -> MaterialTheme.colorScheme.errorContainer
                NotificationStatus.READ -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con icona e timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (notification.type) {
                            NotificationType.EVENT_INVITE -> Icons.Default.GroupAdd
                            NotificationType.EVENT_UPDATE -> Icons.Default.Update
                            NotificationType.EVENT_CANCELLED -> Icons.Default.Cancel
                            NotificationType.GROUP_INVITE -> Icons.Default.GroupAdd
                            NotificationType.GROUP_UPDATE -> Icons.Default.Edit
                            NotificationType.GROUP_DELETED -> Icons.Default.Delete
                            NotificationType.REMOVED_FROM_GROUP -> Icons.Default.PersonRemove
                        },
                        contentDescription = null,
                        tint = when (notification.status) {
                            NotificationStatus.PENDING -> MaterialTheme.colorScheme.onPrimaryContainer
                            NotificationStatus.ACCEPTED -> MaterialTheme.colorScheme.onSecondaryContainer
                            NotificationStatus.DECLINED -> MaterialTheme.colorScheme.onErrorContainer
                            NotificationStatus.READ -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    // Badge status
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when (notification.status) {
                            NotificationStatus.PENDING -> MaterialTheme.colorScheme.primary
                            NotificationStatus.ACCEPTED -> MaterialTheme.colorScheme.secondary
                            NotificationStatus.DECLINED -> MaterialTheme.colorScheme.error
                            NotificationStatus.READ -> MaterialTheme.colorScheme.outline
                        }
                    ) {
                        Text(
                            text = when (notification.status) {
                                NotificationStatus.PENDING -> "Pending"
                                NotificationStatus.ACCEPTED -> "Accepted"
                                NotificationStatus.DECLINED -> "Declined"
                                NotificationStatus.READ -> "Read"
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Titolo
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Messaggio
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium
            )

            // Dettagli evento (se disponibili)
            if (notification.eventDate != null && notification.eventTimeSlot != null) {
                Spacer(modifier = Modifier.height(12.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Data
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Date",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = formatDate(notification.eventDate!!),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Orario
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Time",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = notification.eventTimeSlot!!,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (notification.fieldName != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Field",
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = notification.fieldName!!,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Pulsanti Accetta/Rifiuta (solo per pending)
            if (notification.status == NotificationStatus.PENDING && notification.type == NotificationType.EVENT_INVITE && onAccept != null && onDecline != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        enabled = !isAccepting && !isDeclining,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (isDeclining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Decline")
                        }
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        enabled = !isAccepting && !isDeclining
                    ) {
                        if (isAccepting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Accept")
                        }
                    }
                }
            }

            if (notification.groupName != null) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = "Group",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = notification.groupName!!,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Timestamp
            notification.createdAt?.let { timestamp ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatTimestamp(timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EmptyNotificationsView(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notifications",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatDate(dateString: String): String {
    return try {
        val date = org.threeten.bp.LocalDate.parse(dateString)
        val formatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("dd MMM yyyy")
        date.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        val formatter = DateTimeFormatter.ISO_DATE_TIME

        val cleaned = timestamp.replace("Z", "")
        val dateTime = LocalDateTime.parse(cleaned, formatter)
        val now = LocalDateTime.now()

        val minutes = Duration.between(dateTime, now).toMinutes()

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes minutes ago"
            minutes < 1440 -> "${minutes / 60} hours ago"
            else -> "${minutes / 1440} days ago"
        }

    } catch (e: Exception) {
        timestamp
    }
}