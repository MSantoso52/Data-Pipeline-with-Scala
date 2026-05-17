package com.pipeline.models 

case class RawTransaction(
  id : String, 
  amount: Double, 
  currency: String, 
  timestamp: String
)

case class CleanTransaction(
  transactionId: String,
  totalAmount: Double,
  tax: Double,
  processAt: java.sql.Timestamp
)
