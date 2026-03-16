import request from '../utils/request'

export function addToCart(data) {
  return request({
    url: '/cart/add',
    method: 'post',
    data
  })
}

export function getCart() {
  return request({
    url: '/cart',
    method: 'get'
  })
}

export function updateCartQuantity(id, data) {
  return request({
    url: `/cart/${id}/quantity`,
    method: 'put',
    data
  })
}

export function updateCartChecked(id, data) {
  return request({
    url: `/cart/${id}/checked`,
    method: 'put',
    data
  })
}

export function deleteCartItem(id) {
  return request({
    url: `/cart/${id}`,
    method: 'delete'
  })
}
