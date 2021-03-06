package dtos

import java.util.Date

import dtos.utils.Util
import models.Card
import play.api.Play.current
import play.api.db.DB

import scala.collection.mutable.ListBuffer

object CardDto {

  def save(card: Card): Card = {
    DB.withConnection { conn =>
      val statement = conn.prepareStatement("INSERT INTO Card (question, answer, article, extract) VALUES (?, ?, ?, ?)",
        java.sql.Statement.RETURN_GENERATED_KEYS)
      statement.setString(1, card.question)
      statement.setString(2, card.answer)
      statement.setLong(3, card.article.getOrElse(0))
      statement.setLong(4, card.extract.getOrElse(0))
      statement.executeUpdate()
      val generatedKey = statement.getGeneratedKeys
      if (generatedKey.next()) new Card(generatedKey.getLong(1), card)
      else card
    }
  }

  def update(card: Card): Card = {
    DB.withConnection { conn =>
      val statement = conn.prepareStatement("UPDATE Card SET question=?, answer=?, article=?, extract=? WHERE id=?")
      statement.setString(1, card.question)
      statement.setString(2, card.answer)
      statement.setLong(3, card.article.getOrElse(0))
      statement.setLong(4, card.extract.getOrElse(0))
      statement.setLong(5, card.id)
      statement.executeUpdate()
    }
    card
  }

  def delete(id: Long) = {
    deleteSchedulingInfo(id)
    DB.withConnection { conn =>
      val statement = conn.prepareStatement("DELETE FROM Card WHERE id=?")
      statement.setLong(1, id)
      statement.executeUpdate()
    }
  }

  def get(id: Long): Card = {
    DB.withConnection { conn =>
      val statement = conn.prepareStatement("SELECT * FROM Card WHERE id=?")
      statement.setLong(1, id)
      val resultSet = statement.executeQuery()
      if (resultSet.next()) new Card(resultSet.getLong("id"), resultSet.getString("question"),
        resultSet.getString("answer"),
        Util.fromId(resultSet.getLong("article")), Util.fromId(resultSet.getLong("extract")))
      else null
    }
  }

  def getAll: List[Card] = {
    val cards = new ListBuffer[Card]()
    DB.withConnection { conn =>
      val statement = conn.prepareStatement("SELECT * FROM Card")
      val resultSet = statement.executeQuery()
      while (resultSet.next()) {
        cards += new Card(resultSet.getLong("id"), resultSet.getString("question"),
          resultSet.getString("answer"),
          Util.fromId(resultSet.getLong("article")), Util.fromId(resultSet.getLong("extract")))
      }
    }
    cards.toList
  }

  def getDueTo(date: Date) = {
    val cards = new ListBuffer[Card]()
    DB.withConnection { conn =>
      val statement = conn.prepareStatement("SELECT * FROM Card WHERE id IN (SELECT cardId From SchedulingInfo WHERE nextDate<=?)")
      statement.setDate(1, new java.sql.Date(date.getTime))
      val resultSet = statement.executeQuery()
      while (resultSet.next()) {
        cards += new Card(resultSet.getLong("id"), resultSet.getString("question"),
          resultSet.getString("answer"),
          Util.fromId(resultSet.getLong("article")), Util.fromId(resultSet.getLong("extract")))
      }
    }
    cards.toList
  }

  private def deleteSchedulingInfo(cardId: Long) = {
    DB.withConnection { conn =>
      val statement = conn.prepareStatement("DELETE FROM SchedulingInfo WHERE cardId=?")
      statement.setLong(1, cardId)
      statement.executeUpdate()
    }
  }
}
