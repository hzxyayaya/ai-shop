import request from '../utils/request'

export function buyNow(data) {
  return request({
    url: '/orders/buy-now',
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

export function getOrderDetail(orderNo) {
  return request({
    url: `/orders/${orderNo}`,
    method: 'get'
  })
}
