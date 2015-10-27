package model.user

import com.wordnik.swagger.annotations.{ApiModel, ApiModelProperty}

import scala.annotation.meta.field

@ApiModel(description = "User device")
case class UserDevice(
    @(ApiModelProperty@field)(value = "Device token") device_token: String,
    @(ApiModelProperty@field)(value = "Platform (android|ios)") platform: String)
