/*
 * Copyright (C) 2016 Lightbend Inc. <http://www.lightbend.com>
 */
package exsample

import com.fasterxml.jackson.annotation.JsonIgnore

case class Car @JsonIgnore()(number: String, name: String, status: String) {
  def this(number: String, name: String) = this(number, name, "ready")
}
