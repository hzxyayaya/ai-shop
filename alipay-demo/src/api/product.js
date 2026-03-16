import request from '../utils/request'

export function getProducts(params) {
  return request({
    url: '/products',
    method: 'get',
    params
  })
}
