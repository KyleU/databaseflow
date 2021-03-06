package models.graphql

import models.query.{QueryFilter, RowDataOptions}
import models.result.QueryResultRow
import models.schema.{ColumnType, FilterOp, ForeignKey, Table}
import sangria.schema.{Context, Field, ObjectType, OptionType, Projector}
import services.query.QueryResultRowService
import util.FutureUtils.defaultContext
import util.StringKeyUtils

object ForeignKeyGraphQL {
  def getForeignKeyField(schema: models.schema.Schema, src: Table, tgt: ObjectType[GraphQLContext, QueryResultRow], notNull: Boolean, fk: ForeignKey) = {
    val fkName = if (src.columns.exists(_.name == fk.name)) {
      "fk_" + StringKeyUtils.cleanName(fk.name)
    } else {
      StringKeyUtils.cleanName(fk.name)
    }

    def getFilters(ctx: Context[GraphQLContext, QueryResultRow]) = fk.references.map(r => QueryFilter(
      col = r.target,
      op = FilterOp.Equal,
      t = src.columns.find(_.name.equalsIgnoreCase(r.source)).map(_.columnType).getOrElse(ColumnType.StringType),
      v = ctx.value.getCell(r.source).getOrElse("")
    ))

    if (notNull) {
      Field(
        name = fkName,
        fieldType = tgt,
        description = Some(fk.name),
        resolve = Projector((ctx: Context[GraphQLContext, QueryResultRow], names) => {
          val columns = if (names.exists(n => !src.columns.exists(_.name == n.name))) { Seq("*") } else { names.map(_.name) }
          val rdo = RowDataOptions(filters = getFilters(ctx))
          QueryResultRowService.getTableData(ctx.ctx.user, schema.connectionId, fk.targetTable, columns, rdo).map(_.head)
        })
      )
    } else {
      Field(
        name = fkName,
        fieldType = OptionType(tgt),
        description = Some(fk.name),
        resolve = Projector((ctx: Context[GraphQLContext, QueryResultRow], names) => {
          val columns = if (names.exists(n => !src.columns.exists(_.name == n.name))) { Seq("*") } else { names.map(_.name) }
          val rdo = RowDataOptions(filters = getFilters(ctx))
          QueryResultRowService.getTableData(ctx.ctx.user, schema.connectionId, fk.targetTable, columns, rdo).map(_.headOption)
        })
      )
    }
  }
}
