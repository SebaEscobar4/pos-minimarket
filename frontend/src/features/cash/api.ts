import { mutateJson, readJson } from '../../shared/api/client'

export type CashSession = {
  id: string
  openedBy: string
  openedByDisplayName: string
  openedAt: string
  openingAmount: number
  status: 'OPEN' | 'CLOSED'
}

export type ManualCashDirection = 'INCOME' | 'WITHDRAWAL'

export type CashMovementCategory =
  | 'CASH_REPLENISHMENT'
  | 'OTHER_INCOME'
  | 'SUPPLIER_PAYMENT'
  | 'OPERATING_EXPENSE'
  | 'SAFE_DROP'
  | 'OTHER_WITHDRAWAL'

export type CashMovement = {
  id: string
  cashSessionId: string
  type: 'CASH_SALE' | 'MANUAL_INCOME' | 'MANUAL_WITHDRAWAL' | 'CASH_REFUND'
  category: CashMovementCategory | null
  amount: number
  reason: string | null
  reference: string | null
  actorId: string
  actorDisplayName: string
  occurredAt: string
}

export type CashTotals = {
  cashSales: number
  manualIncome: number
  manualWithdrawals: number
  cashRefunds: number
  cardSales: number
  transferSales: number
}

export type CashSummary = {
  session: CashSession
  totals: CashTotals
  expectedCash: number
  movements: {
    items: CashMovement[]
    page: number
    size: number
    totalElements: number
    totalPages: number
  }
}

export type CashCloseResult = {
  sessionId: string
  closedBy: string
  closedByDisplayName: string
  closedAt: string
  openingAmount: number
  expectedCash: number
  countedCash: number
  difference: number
  totals: CashTotals
}

type CurrentCashSessionResponse = {
  session: CashSession | null
}

export async function currentCashSession(): Promise<CashSession | null> {
  const response = await fetch('/api/v1/cash/sessions/current', {
    credentials: 'same-origin',
  })
  return (await readJson<CurrentCashSessionResponse>(response)).session
}

export function openCashSession(openingAmount: number): Promise<CashSession> {
  return mutateJson('/api/v1/cash/sessions', 'POST', { openingAmount })
}

export async function currentCashSummary(): Promise<CashSummary> {
  return readJson(
    await fetch('/api/v1/cash/sessions/current/summary?page=0&size=100', {
      credentials: 'same-origin',
    }),
  )
}

export function recordManualCashMovement(
  direction: ManualCashDirection,
  category: CashMovementCategory,
  amount: number,
  reason: string,
): Promise<CashMovement> {
  return mutateJson('/api/v1/admin/cash/movements', 'POST', {
    direction,
    category,
    amount,
    reason,
  })
}

export function closeCashSession(countedCash: number): Promise<CashCloseResult> {
  return mutateJson('/api/v1/cash/sessions/current/close', 'POST', { countedCash })
}
