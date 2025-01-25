### 前端页面命名规范

#### vue页面js方法命名

##### 所有方法请勿使用复数 （customers）

- //导出客户excel
- exportCustomerToExcel()
- //客户导入表单
- showCustomerImportForm()
- //选择默认客户分类
- selectDefaultCustomerCategory()
- //客户分类change事件
- onCustomerCategoryChange(data)
- //客户分类表单
- showCustomerCategoryForm(entity)
- //删除客户分类
- deleteCustomerCategory(row)
- //查询客户
- searchCustomer()
- //客户表单
- showCustomerForm(entity)
- //删除客户
- deleteCustomer(row)
- //加载客户
- loadCustomer()
- //加载客户分类
- loadCustomerCategory()
- //查询参数
- queryParams()

#### vue页面变量命名

- //查询参数
- params
- //客户分类列表
- customerCategoryDataList
- //客户列表
- customerDataList

#### API方法命名(Customer.js)

##### 方法名请去掉实体名称

- //保存or编辑
- save
- //列表
- list
- //删除
- delete
- //选择
- select
- //导入
- importData
- //导出excel
- exportToExcel

#### Controller/service方法命名(CustomerController/CustomerService)

- controller/service命名请保持一致