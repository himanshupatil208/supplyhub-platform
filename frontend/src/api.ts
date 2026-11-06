
const BASE = import.meta.env.VITE_API_BASE_URL || ''

export async function api<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  })

  if (!response.ok) {
    let message = `Request failed (${response.status})`

    try {
      const body = await response.json()
      message = body.message || body.error || message
    } catch {}

    throw new Error(message)
  }

  if (response.status === 204) return undefined as T

  return response.json()
}

export type Product = {
  id: number
  sku: string
  name: string
  description: string
  category: string
  unitPrice: number
  active: boolean
}

export type Stock = {
  id: number
  sku: string
  productName: string
  availableQuantity: number
  reservedQuantity: number
  reorderLevel: number
  warehouse: string
  lowStock: boolean
}

export type Customer = {
  id: number
  name: string
  email: string
  phone: string
  company: string
  status: string
}

export type Order = {
  id: number
  customerId: number
  customerName: string
  status: string
  total: number
  createdAt: string
  items: {
    sku: string
    productName: string
    quantity: number
    unitPrice: number
    lineTotal: number
  }[]
}

export type Notice = {
  id: number
  type: string
  message: string
  referenceId: string
  createdAt: string
}


