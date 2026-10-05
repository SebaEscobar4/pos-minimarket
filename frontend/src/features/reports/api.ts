import { readJson } from '../../shared/api/client'

export type PaymentMethod = 'CASH' | 'CARD' | 'TRANSFER'
export type InventoryMovementType =
  | 'ENTRY'
  | 'SALE_OUT'
  | 'ADJUSTMENT_IN'
  | 'ADJUSTMENT_OUT'
  | 'DAMAGED'
  | 'EXPIRED'
  | 'SALE_REVERSAL'

export type ReportPage<T> = {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type SalesReport = {
  from: string
  to: string
  methodFilter: PaymentMethod | null
  recordedSales: number
  recordedAmount: number
  voidedSales: number
  voidedAmount: number
  netSales: number
  netAmount: number
  estimatedGrossProfit: number
  payments: {
    method: PaymentMethod
    recordedSales: number
    recordedAmount: number
    voidedSales: number
    voidedAmount: number
    netSales: number
    netAmount: number
  }[]
  refunds: {
    method: PaymentMethod
    cancellations: number
    exactAmount: number
    payableAmount: number
  }[]
  topProducts: {
    productId: string
    code: string | null
    name: string
    quantity: number
    revenue: number
  }[]
}

export type InventoryReport = {
  from: string
  to: string
  typeFilter: InventoryMovementType | null
  productFilter: string | null
  lowStock: {
    productId: string
    code: string | null
    name: string
    quantity: number
    minimumStock: number
    shortage: number
  }[]
  totals: {
    type: InventoryMovementType
    movements: number
    quantity: number
    netDelta: number
  }[]
  movements: ReportPage<{
    id: string
    productId: string
    productCode: string | null
    productName: string
    type: InventoryMovementType
    quantity: number
    delta: number
    previousBalance: number
    resultingBalance: number
    reason: string | null
    reference: string | null
    actorDisplayName: string
    occurredAt: string
  }>
}

export type CashReport = {
  from: string
  to: string
  openedSessions: number
  closedSessions: number
  openingAmount: number
  expectedCash: number
  countedCash: number
  difference: number
  sessions: ReportPage<{
    id: string
    status: 'OPEN' | 'CLOSED'
    openedBy: string
    openedAt: string
    openingAmount: number
    closedBy: string | null
    closedAt: string | null
    expectedCash: number | null
    countedCash: number | null
    difference: number | null
  }>
}

export type ReportFilters = {
  from: string
  to: string
}

export async function getSalesReport(
  filters: ReportFilters & { method?: PaymentMethod },
): Promise<SalesReport> {
  return get<SalesReport>('/api/v1/admin/reports/sales', filters)
}

export async function getInventoryReport(
  filters: ReportFilters & {
    type?: InventoryMovementType
    productId?: string
    page?: number
    size?: number
  },
): Promise<InventoryReport> {
  return get<InventoryReport>('/api/v1/admin/reports/inventory', filters)
}

export async function getCashReport(
  filters: ReportFilters & { page?: number; size?: number },
): Promise<CashReport> {
  return get<CashReport>('/api/v1/admin/reports/cash', filters)
}

async function get<T>(
  path: string,
  values: Record<string, string | number | undefined>,
): Promise<T> {
  const parameters = new URLSearchParams()
  Object.entries(values).forEach(([key, value]) => {
    if (value !== undefined && value !== '') parameters.set(key, String(value))
  })
  return readJson(
    await fetch(`${path}?${parameters.toString()}`, {
      credentials: 'same-origin',
    }),
  )
}
