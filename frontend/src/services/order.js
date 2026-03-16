import request from '../utils/request'

export function buyNow(data) {
  return request({
    url: '/orders/buy-now',
    method: 'post',
    data
  })
}

export function checkout(data) {
  return request({
    url: '/orders/checkout',
    method: 'post',
    data
  })
}

export function getOrders(params) {
  return request({
    url: '/orders',
    method: 'get',
    params
  })
}

export function getOrderDetail(orderNo, config = {}) {
  return request({
    url: `/orders/${orderNo}`,
    method: 'get',
    ...config
  })
}

export function deleteOrder(orderNo) {
  return request({
    url: `/orders/${orderNo}`,
    method: 'delete'
  })
}
