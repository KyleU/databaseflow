package models.settings

import enumeratum._

sealed abstract class SettingKey(val id: String, val title: String, val description: String, val default: String) extends EnumEntry {
  override def toString = id
}

object SettingKey extends Enum[SettingKey] with CirceEnum[SettingKey] {
  case object AllowRegistration extends SettingKey(
    id = "allow-registration",
    title = "Allow Registration",
    description = "Determines if users are allowed to sign up.",
    default = "true"
  )

  case object AllowSignIn extends SettingKey(
    id = "allow-sign-in",
    title = "Allow Sign In",
    description = "When turned off, only administrators may sign in.",
    default = "true"
  )

  case object DefaultNewUserRole extends SettingKey(
    id = "default-new-user-role",
    title = "Default New User Role",
    description = "Determines the role to assign new users.",
    default = "user"
  )

  case object AddConnectionRole extends SettingKey(
    id = "add-connection-role",
    title = "Add Connection Role",
    description = "The role required to create new connections.",
    default = "user"
  )

  case object AllowAuditRemoval extends SettingKey(
    id = "allow-audit-removal",
    title = "Allow Audit Removal",
    description = "Determines if users are allowed to remove audit records from the system.",
    default = "true"
  )

  case object MessageOfTheDay extends SettingKey(
    id = "motd",
    title = "Message of The Day",
    description = "When set, displays the content on the sign in, registration, and home screens.",
    default = ""
  )

  case object InstallId extends SettingKey(
    id = "install-id",
    title = "Install ID",
    description = "The id of this installation. Please don't mess with this.",
    default = ""
  )

  case object InstallDate extends SettingKey(
    id = "install-date",
    title = "Install Date",
    description = "The day the software was installed. Please don't mess with this.",
    default = ""
  )

  case object Invalid extends SettingKey(
    id = "invalid",
    title = "Invalid Config Key",
    description = "You shouldn't be seeing this.",
    default = ""
  )

  override val values = findValues
}
