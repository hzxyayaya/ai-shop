import request from '../utils/request'

export function createPayment(data) {
  return request({
    url: '/payments/create',
    method: 'post',
    data
  })
}

export function getPaymentStatus(orderNo) {
  return request({
    url: `/payments/${orderNo}/status`,
    method: 'get'
  })
}
